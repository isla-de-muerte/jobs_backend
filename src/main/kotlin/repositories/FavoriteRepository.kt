package repositories

import db.FavoriteVacancies
import db.Vacancies
import db.dbQuery
import domain.VacancyStatus
import dto.FavoriteResponse
import dto.VacancyResponse
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.util.UUID

class FavoriteRepository {

    suspend fun addFavorite(
        applicantId: UUID,
        vacancyId: UUID
    ): FavoriteResponse = dbQuery {

        val vacancyExists = Vacancies
            .selectAll()
            .where {
                (Vacancies.id eq vacancyId) and
                        (Vacancies.status eq VacancyStatus.PUBLISHED)
            }
            .singleOrNull()
            ?: throw IllegalArgumentException("Published vacancy not found")

        val existing = FavoriteVacancies
            .selectAll()
            .where {
                (FavoriteVacancies.applicantId eq applicantId) and
                        (FavoriteVacancies.vacancyId eq vacancyId)
            }
            .singleOrNull()

        if (existing != null) {
            return@dbQuery existing.toFavoriteResponse()
        }

        val id = FavoriteVacancies.insertAndGetId {
            it[FavoriteVacancies.applicantId] = applicantId
            it[FavoriteVacancies.vacancyId] = vacancyId
        }

        FavoriteVacancies
            .selectAll()
            .where { FavoriteVacancies.id eq id }
            .single()
            .toFavoriteResponse()
    }

    suspend fun removeFavorite(
        applicantId: UUID,
        vacancyId: UUID
    ) = dbQuery {
        FavoriteVacancies.deleteWhere {
            (FavoriteVacancies.applicantId eq applicantId) and
                    (FavoriteVacancies.vacancyId eq vacancyId)
        }

        Unit
    }

    suspend fun getFavorites(
        applicantId: UUID
    ): List<VacancyResponse> = dbQuery {

        FavoriteVacancies
            .innerJoin(Vacancies)
            .selectAll()
            .where {
                FavoriteVacancies.applicantId eq applicantId
            }
            .orderBy(FavoriteVacancies.createdAt to SortOrder.DESC)
            .map { row ->
                VacancyResponse(
                    id = row[Vacancies.id].value.toString(),
                    employerId = row[Vacancies.employerId].value.toString(),
                    categoryId = row[Vacancies.categoryId].value.toString(),
                    title = row[Vacancies.title],
                    description = row[Vacancies.description],
                    requirements = row[Vacancies.requirements],
                    salaryFrom = row[Vacancies.salaryFrom],
                    salaryTo = row[Vacancies.salaryTo],
                    workFormat = dto.WorkFormatDto.valueOf(row[Vacancies.workFormat].name),
                    status = dto.VacancyStatusDto.valueOf(row[Vacancies.status].name),
                    viewsCount = row[Vacancies.viewsCount],
                    applicationsCount = row[Vacancies.applicationsCount],
                    createdAt = row[Vacancies.createdAt].toString(),
                    updatedAt = row[Vacancies.updatedAt].toString()
                )
            }
    }

    private fun ResultRow.toFavoriteResponse(): FavoriteResponse {
        return FavoriteResponse(
            id = this[FavoriteVacancies.id].value.toString(),
            vacancyId = this[FavoriteVacancies.vacancyId].value.toString(),
            createdAt = this[FavoriteVacancies.createdAt].toString()
        )
    }
}