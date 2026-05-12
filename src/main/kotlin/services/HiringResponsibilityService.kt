package services

import db.Applications
import db.Vacancies
import db.dbQuery
import domain.AppException
import domain.ApplicationStatus
import domain.VacancyStatus
import io.ktor.http.HttpStatusCode
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.innerJoin
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

class HiringResponsibilityService {

    companion object {
        /**
         * true — тестовый режим:
         * - перегруз после 2 откликов
         * - автоархив через 3 минуты
         * - "старые" необработанные отклики считаются уже через 5 минут
         *
         * false — нормальный режим:
         * - перегруз после 50 откликов
         * - автоархив через 3 дня
         * - "старые" необработанные отклики считаются через 7 дней
         */
        private const val TEST_MODE = true

        /**
         * Минимальное общее количество откликов на вакансию,
         * после которого начинаем проверять перегруз.
         */
        private val OVERLOAD_TOTAL_APPLICATIONS =
            if (TEST_MODE) 2L else 50L

        /**
         * Сколько новых откликов должно прийти за короткое окно,
         * чтобы считать вакансию перегруженной.
         */
        private val OVERLOAD_MIN_RECENT_APPLICATIONS =
            if (TEST_MODE) 2L else 20L

        /**
         * Окно времени для проверки резкого притока откликов.
         */
        private val OVERLOAD_WINDOW_AMOUNT =
            if (TEST_MODE) 30L else 30L

        private val OVERLOAD_WINDOW_UNIT =
            ChronoUnit.MINUTES

        /**
         * Если обработано меньше 30%, и выполнены остальные условия,
         * вакансия уходит в PAUSED_BY_OVERLOAD.
         */
        private const val OVERLOAD_MAX_PROCESSED_RATE = 0.30

        /**
         * Чтобы снова открыть PAUSED_BY_OVERLOAD вакансию,
         * нужно обработать минимум 50% откликов.
         */
        private const val RESUME_REQUIRED_PROCESSED_RATE = 0.50

        /**
         * Через сколько времени PAUSED_BY_OVERLOAD вакансия архивируется,
         * если по ней не обработали ни одного отклика.
         *
         * TEST_MODE: 3 минуты.
         * NORMAL: 3 дня.
         */
        private val ABANDONED_PAUSE_AMOUNT =
            if (TEST_MODE) 3L else 3L

        private val ABANDONED_PAUSE_UNIT =
            if (TEST_MODE) ChronoUnit.MINUTES else ChronoUnit.DAYS

        /**
         * Через сколько времени NEW-отклик считается старым
         * для системной блокировки публикации новых вакансий.
         *
         * TEST_MODE: 5 минут.
         * NORMAL: 7 дней.
         */
        private val STALE_APPLICATIONS_AMOUNT =
            if (TEST_MODE) 5L else 7L

        private val STALE_APPLICATIONS_UNIT =
            if (TEST_MODE) ChronoUnit.MINUTES else ChronoUnit.DAYS

        /**
         * Если старых NEW-откликов меньше этого лимита,
         * публикация новых вакансий разрешена.
         *
         * TEST_MODE: 2 старых NEW отклика.
         * NORMAL: 30 старых NEW откликов.
         */
        private val STALE_NEW_APPLICATIONS_LIMIT =
            if (TEST_MODE) 2L else 30L

        /**
         * Если общий процент обработанных откликов >= 20%,
         * публикация разрешена.
         */
        private const val MIN_OVERALL_PROCESSED_RATE = 0.20
    }

    /**
     * Проверяет конкретную вакансию после нового отклика.
     *
     * Если откликов слишком много, они пришли быстро,
     * а работодатель обработал слишком маленький процент,
     * вакансия переводится:
     *
     * PUBLISHED -> PAUSED_BY_OVERLOAD
     */
    suspend fun pauseVacancyIfOverloaded(
        vacancyId: UUID
    ) = dbQuery {
        val totalApplications = Applications
            .selectAll()
            .where {
                Applications.vacancyId eq vacancyId
            }
            .count()

        if (totalApplications < OVERLOAD_TOTAL_APPLICATIONS) {
            return@dbQuery
        }

        val newApplications = Applications
            .selectAll()
            .where {
                (Applications.vacancyId eq vacancyId) and
                        (Applications.status eq ApplicationStatus.NEW)
            }
            .count()

        val processedApplications = totalApplications - newApplications

        val processedRate =
            processedApplications.toDouble() / totalApplications.toDouble()

        val recentBorder = Instant.now().minus(
            OVERLOAD_WINDOW_AMOUNT,
            OVERLOAD_WINDOW_UNIT
        )

        val recentNewApplications = Applications
            .selectAll()
            .where {
                (Applications.vacancyId eq vacancyId) and
                        (Applications.status eq ApplicationStatus.NEW) and
                        (Applications.createdAt greaterEq recentBorder)
            }
            .count()

        val shouldPause =
            totalApplications >= OVERLOAD_TOTAL_APPLICATIONS &&
                    processedRate < OVERLOAD_MAX_PROCESSED_RATE &&
                    recentNewApplications >= OVERLOAD_MIN_RECENT_APPLICATIONS

        if (shouldPause) {
            val now = Instant.now()

            Vacancies.update({
                (Vacancies.id eq vacancyId) and
                        (Vacancies.status eq VacancyStatus.PUBLISHED)
            }) { row ->
                row[Vacancies.status] = VacancyStatus.PAUSED_BY_OVERLOAD
                row[Vacancies.pausedAt] = now
                row[Vacancies.updatedAt] = now
            }
        }
    }

    /**
     * Проверяет, может ли работодатель публиковать новую вакансию.
     *
     * Блокировка включается только если одновременно:
     *
     * 1. Старых NEW-откликов достаточно много.
     * 2. Общий процент обработки меньше 20%.
     * 3. За последние N минут/дней не было ни одной обработки.
     *
     * В TEST_MODE N = 5 минут.
     * В NORMAL N = 7 дней.
     *
     * Если у работодателя 0 откликов — публикация разрешена.
     */
    suspend fun ensureEmployerCanPublish(
        employerId: UUID
    ) = dbQuery {
        val staleBorder = Instant.now().minus(
            STALE_APPLICATIONS_AMOUNT,
            STALE_APPLICATIONS_UNIT
        )

        val oldNewApplications = Applications
            .innerJoin(Vacancies)
            .selectAll()
            .where {
                (Vacancies.employerId eq employerId) and
                        (Applications.status eq ApplicationStatus.NEW) and
                        (Applications.createdAt lessEq staleBorder)
            }
            .count()

        if (oldNewApplications < STALE_NEW_APPLICATIONS_LIMIT) {
            return@dbQuery
        }

        val totalApplications = Applications
            .innerJoin(Vacancies)
            .selectAll()
            .where {
                Vacancies.employerId eq employerId
            }
            .count()

        if (totalApplications == 0L) {
            return@dbQuery
        }

        val newApplications = Applications
            .innerJoin(Vacancies)
            .selectAll()
            .where {
                (Vacancies.employerId eq employerId) and
                        (Applications.status eq ApplicationStatus.NEW)
            }
            .count()

        val processedApplications = totalApplications - newApplications

        val overallProcessedRate =
            processedApplications.toDouble() / totalApplications.toDouble()

        if (overallProcessedRate >= MIN_OVERALL_PROCESSED_RATE) {
            return@dbQuery
        }

        val processedLastPeriod = Applications
            .innerJoin(Vacancies)
            .selectAll()
            .where {
                (Vacancies.employerId eq employerId) and
                        (Applications.status neq ApplicationStatus.NEW) and
                        (Applications.updatedAt greaterEq staleBorder)
            }
            .count()

        if (processedLastPeriod > 0) {
            return@dbQuery
        }

        throw AppException(
            status = HttpStatusCode.BadRequest,
            code = "HIRING_RESPONSIBILITY_REQUIRED",
            message = if (TEST_MODE) {
                "Публикация временно ограничена: у вас много необработанных откликов старше 5 минут и за последние 5 минут не было обработки кандидатов."
            } else {
                "Публикация временно ограничена: у вас много старых необработанных откликов и за последние 7 дней не было обработки кандидатов."
            }
        )
    }

    /**
     * Проверяет, можно ли снова открыть вакансию,
     * которая была поставлена на паузу из-за перегруза.
     *
     * Нужно обработать минимум 50% откликов.
     */
    suspend fun ensureVacancyCanBeResumed(
        vacancyId: UUID
    ) = dbQuery {
        val totalApplications = Applications
            .selectAll()
            .where {
                Applications.vacancyId eq vacancyId
            }
            .count()

        if (totalApplications == 0L) {
            return@dbQuery
        }

        val newApplications = Applications
            .selectAll()
            .where {
                (Applications.vacancyId eq vacancyId) and
                        (Applications.status eq ApplicationStatus.NEW)
            }
            .count()

        val processedApplications = totalApplications - newApplications

        val processedRate =
            processedApplications.toDouble() / totalApplications.toDouble()

        if (processedRate < RESUME_REQUIRED_PROCESSED_RATE) {
            throw AppException(
                status = HttpStatusCode.BadRequest,
                code = "VACANCY_STILL_OVERLOADED",
                message = "Чтобы снова открыть вакансию, обработайте минимум 50% откликов."
            )
        }
    }

    /**
     * Архивирует PAUSED_BY_OVERLOAD вакансии,
     * если они слишком долго стоят на паузе
     * и работодатель не обработал по ним ни одного отклика.
     *
     * TEST_MODE:
     * - через 3 минуты.
     *
     * NORMAL:
     * - через 3 дня.
     */
    suspend fun archiveAbandonedPausedVacancies() = dbQuery {
        val border = Instant.now().minus(
            ABANDONED_PAUSE_AMOUNT,
            ABANDONED_PAUSE_UNIT
        )

        val pausedVacancies = Vacancies
            .selectAll()
            .where {
                (Vacancies.status eq VacancyStatus.PAUSED_BY_OVERLOAD) and
                        (Vacancies.pausedAt lessEq border)
            }
            .toList()

        pausedVacancies.forEach { vacancyRow ->
            val vacancyId = vacancyRow[Vacancies.id].value
            val pausedAt = vacancyRow[Vacancies.pausedAt] ?: return@forEach

            val processedAfterPause = Applications
                .selectAll()
                .where {
                    (Applications.vacancyId eq vacancyId) and
                            (Applications.status neq ApplicationStatus.NEW) and
                            (Applications.updatedAt greaterEq pausedAt)
                }
                .count()

            if (processedAfterPause == 0L) {
                val archiveTime = Instant.now()

                Vacancies.update({
                    Vacancies.id eq vacancyId
                }) { row ->
                    row[Vacancies.status] = VacancyStatus.ARCHIVED
                    row[Vacancies.pausedAt] = null
                    row[Vacancies.updatedAt] = archiveTime
                }
            }
        }
    }
}