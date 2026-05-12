package repositories

import db.Vacancies
import db.dbQuery
import domain.VacancyStatus
import domain.WorkFormat
import dto.VacancyCreateRequest
import dto.VacancyFilterParams
import dto.VacancyListResponse
import dto.VacancyResponse
import dto.VacancyStatusDto
import dto.WorkFormatDto
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import org.jetbrains.exposed.sql.lowerCase
import java.time.Instant
import java.util.UUID

class VacancyRepository {

    suspend fun create(
        employerId: UUID,
        request: VacancyCreateRequest
    ): VacancyResponse = dbQuery {
        val now = Instant.now()

        val vacancyId = Vacancies.insertAndGetId {
            it[Vacancies.employerId] = employerId
            it[Vacancies.categoryId] = UUID.fromString(request.categoryId)
            it[Vacancies.title] = request.title
            it[Vacancies.description] = request.description
            it[Vacancies.requirements] = request.requirements
            it[Vacancies.salaryFrom] = request.salaryFrom
            it[Vacancies.salaryTo] = request.salaryTo
            it[Vacancies.workFormat] = request.workFormat.toDomain()
            it[Vacancies.status] = VacancyStatus.DRAFT
            it[Vacancies.createdAt] = now
            it[Vacancies.updatedAt] = now
        }

        Vacancies
            .selectAll()
            .where { Vacancies.id eq vacancyId }
            .single()
            .toResponse()
    }

    suspend fun findEmployerVacancies(
        employerId: UUID
    ): List<VacancyResponse> = dbQuery {
        Vacancies
            .selectAll()
            .where { Vacancies.employerId eq employerId }
            .orderBy(Vacancies.createdAt to SortOrder.DESC)
            .map { row -> row.toResponse() }
    }

    suspend fun findById(
        id: UUID
    ): VacancyResponse? = dbQuery {
        Vacancies
            .selectAll()
            .where { Vacancies.id eq id }
            .singleOrNull()
            ?.toResponse()
    }

    suspend fun publish(
        id: UUID
    ) = dbQuery {
        Vacancies.update({ Vacancies.id eq id }) {
            it[Vacancies.status] = VacancyStatus.PUBLISHED
            it[Vacancies.publishedAt] = Instant.now()
            it[Vacancies.pausedAt] = null
            it[Vacancies.updatedAt] = Instant.now()
        }

        Unit
    }

    suspend fun archive(
        id: UUID
    ) = dbQuery {
        Vacancies.update({ Vacancies.id eq id }) {
            it[Vacancies.status] = VacancyStatus.ARCHIVED
            it[Vacancies.pausedAt] = null
            it[Vacancies.updatedAt] = Instant.now()
        }

        Unit
    }

    suspend fun resumeAfterOverload(
        id: UUID
    ) = dbQuery {
        Vacancies.update({
            (Vacancies.id eq id) and
                    (Vacancies.status eq VacancyStatus.PAUSED_BY_OVERLOAD)
        }) {
            it[Vacancies.status] = VacancyStatus.PUBLISHED
            it[Vacancies.pausedAt] = null
            it[Vacancies.updatedAt] = Instant.now()
        }

        Unit
    }

    suspend fun publicVacancies(
        params: VacancyFilterParams
    ): Pair<List<VacancyResponse>, Long> = dbQuery {
        var query = Vacancies
            .selectAll()
            .where {
                Vacancies.status eq VacancyStatus.PUBLISHED
            }

        params.categoryId?.let { categoryId ->
            query = query.andWhere {
                Vacancies.categoryId eq UUID.fromString(categoryId)
            }
        }

        params.workFormat?.let { format ->
            query = query.andWhere {
                Vacancies.workFormat eq format.toDomain()
            }
        }

        params.query
            ?.takeIf { it.isNotBlank() }
            ?.let { search ->
                query = query.andWhere {
                    Vacancies.title.lowerCase() like "%${search.lowercase()}%"
                }
            }

        val total = query.count()

        val order = when (params.sort) {
            "createdAt_asc" -> Vacancies.createdAt to SortOrder.ASC
            else -> Vacancies.createdAt to SortOrder.DESC
        }

        val offset = ((params.page - 1) * params.size).toLong()

        val items = query
            .orderBy(order)
            .limit(params.size)
            .offset(offset)
            .map { row -> row.toResponse() }

        items to total
    }

    suspend fun incrementViews(
        id: UUID
    ) = dbQuery {
        Vacancies.update({ Vacancies.id eq id }) {
            with(SqlExpressionBuilder) {
                it.update(
                    Vacancies.viewsCount,
                    Vacancies.viewsCount + 1
                )
            }
        }

        Unit
    }

    private fun ResultRow.toResponse(): VacancyResponse {
        return VacancyResponse(
            id = this[Vacancies.id].value.toString(),
            employerId = this[Vacancies.employerId].value.toString(),
            categoryId = this[Vacancies.categoryId].value.toString(),
            title = this[Vacancies.title],
            description = this[Vacancies.description],
            requirements = this[Vacancies.requirements],
            salaryFrom = this[Vacancies.salaryFrom],
            salaryTo = this[Vacancies.salaryTo],
            workFormat = this[Vacancies.workFormat].toDto(),
            status = this[Vacancies.status].toDto(),
            viewsCount = this[Vacancies.viewsCount],
            applicationsCount = this[Vacancies.applicationsCount],
            createdAt = this[Vacancies.createdAt].toString(),
            updatedAt = this[Vacancies.updatedAt].toString()
        )
    }

    private fun WorkFormatDto.toDomain(): WorkFormat {
        return WorkFormat.valueOf(name)
    }

    private fun WorkFormat.toDto(): WorkFormatDto {
        return WorkFormatDto.valueOf(name)
    }

    private fun VacancyStatus.toDto(): VacancyStatusDto {
        return VacancyStatusDto.valueOf(name)
    }
}