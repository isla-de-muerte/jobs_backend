package repositories

import db.ApplicantProfiles
import db.Applications
import db.Vacancies
import db.dbQuery
import domain.ApplicationStatus
import domain.VacancyStatus
import dto.ApplicantProfileResponse
import dto.ApplicationResponse
import dto.EmployerApplicationResponse
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.Instant
import java.util.UUID

class ApplicationRepository {

    suspend fun applyToVacancy(
        vacancyId: UUID,
        applicantId: UUID,
        coverLetter: String?
    ): ApplicationResponse = dbQuery {
        Vacancies
            .selectAll()
            .where {
                (Vacancies.id eq vacancyId) and
                        (Vacancies.status eq VacancyStatus.PUBLISHED)
            }
            .singleOrNull()
            ?: throw IllegalArgumentException("Vacancy is not available for applications")

        val profile = ApplicantProfiles
            .selectAll()
            .where { ApplicantProfiles.userId eq applicantId }
            .singleOrNull()
            ?: throw IllegalArgumentException("Applicant profile is required before applying")

        val existing = Applications
            .selectAll()
            .where {
                (Applications.vacancyId eq vacancyId) and
                        (Applications.applicantId eq applicantId)
            }
            .singleOrNull()

        if (existing != null) {
            throw IllegalArgumentException("You have already applied to this vacancy")
        }

        val now = Instant.now()

        val applicationId = Applications.insertAndGetId {
            it[Applications.vacancyId] = vacancyId
            it[Applications.applicantId] = applicantId
            it[Applications.profileId] = profile[ApplicantProfiles.id].value
            it[Applications.coverLetter] = coverLetter
            it[Applications.status] = ApplicationStatus.NEW
            it[Applications.createdAt] = now
            it[Applications.updatedAt] = now
        }

        Vacancies.update({ Vacancies.id eq vacancyId }) {
            with(SqlExpressionBuilder) {
                it.update(
                    Vacancies.applicationsCount,
                    Vacancies.applicationsCount + 1
                )
            }
        }

        Applications
            .selectAll()
            .where { Applications.id eq applicationId }
            .single()
            .toApplicationResponse()
    }

    suspend fun findApplicantApplications(
        applicantId: UUID
    ): List<ApplicationResponse> = dbQuery {
        Applications
            .selectAll()
            .where { Applications.applicantId eq applicantId }
            .orderBy(Applications.createdAt to SortOrder.DESC)
            .map { row -> row.toApplicationResponse() }
    }

    suspend fun findEmployerApplicationsByVacancy(
        employerId: UUID,
        vacancyId: UUID
    ): List<EmployerApplicationResponse> = dbQuery {
        val vacancy = Vacancies
            .selectAll()
            .where {
                (Vacancies.id eq vacancyId) and
                        (Vacancies.employerId eq employerId)
            }
            .singleOrNull()
            ?: throw IllegalArgumentException("Vacancy not found or access denied")

        Applications
            .innerJoin(ApplicantProfiles)
            .selectAll()
            .where { Applications.vacancyId eq vacancy[Vacancies.id].value }
            .orderBy(Applications.createdAt to SortOrder.DESC)
            .map { row ->
                EmployerApplicationResponse(
                    id = row[Applications.id].value.toString(),
                    vacancyId = row[Applications.vacancyId].value.toString(),
                    applicantId = row[Applications.applicantId].value.toString(),
                    coverLetter = row[Applications.coverLetter],
                    status = row[Applications.status].name,
                    createdAt = row[Applications.createdAt].toString(),
                    resume = ApplicantProfileResponse(
                        id = row[ApplicantProfiles.id].value.toString(),
                        userId = row[ApplicantProfiles.userId].value.toString(),
                        fullName = row[ApplicantProfiles.fullName],
                        contacts = row[ApplicantProfiles.contacts],
                        skills = row[ApplicantProfiles.skills]
                            .split(",")
                            .map { it.trim() }
                            .filter { it.isNotBlank() },
                        experience = row[ApplicantProfiles.experience],
                        education = row[ApplicantProfiles.education],
                        portfolioUrl = row[ApplicantProfiles.portfolioUrl],
                        createdAt = row[ApplicantProfiles.createdAt].toString(),
                        updatedAt = row[ApplicantProfiles.updatedAt].toString()
                    )
                )
            }
    }

    suspend fun updateStatusByEmployer(
        employerId: UUID,
        applicationId: UUID,
        status: ApplicationStatus
    ): ApplicationResponse = dbQuery {
        Applications
            .innerJoin(Vacancies)
            .selectAll()
            .where {
                (Applications.id eq applicationId) and
                        (Vacancies.employerId eq employerId)
            }
            .singleOrNull()
            ?: throw IllegalArgumentException("Application not found or access denied")

        Applications.update({ Applications.id eq applicationId }) {
            it[Applications.status] = status
            it[Applications.updatedAt] = Instant.now()
        }

        Applications
            .selectAll()
            .where { Applications.id eq applicationId }
            .single()
            .toApplicationResponse()
    }

    private fun ResultRow.toApplicationResponse(): ApplicationResponse {
        return ApplicationResponse(
            id = this[Applications.id].value.toString(),
            vacancyId = this[Applications.vacancyId].value.toString(),
            applicantId = this[Applications.applicantId].value.toString(),
            profileId = this[Applications.profileId].value.toString(),
            coverLetter = this[Applications.coverLetter],
            status = this[Applications.status].name,
            createdAt = this[Applications.createdAt].toString(),
            updatedAt = this[Applications.updatedAt].toString()
        )
    }
}