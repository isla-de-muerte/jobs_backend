package repositories

import db.ApplicantProfiles
import db.dbQuery
import dto.ApplicantProfileRequest
import dto.ApplicantProfileResponse
import org.jetbrains.exposed.sql.*
import java.time.Instant
import java.util.UUID

class ProfileRepository {

    suspend fun findApplicantProfile(userId: UUID): ApplicantProfileResponse? = dbQuery {
        ApplicantProfiles
            .selectAll()
            .where { ApplicantProfiles.userId eq userId }
            .singleOrNull()
            ?.toResponse()
    }

    suspend fun upsertApplicantProfile(
        userId: UUID,
        request: ApplicantProfileRequest
    ): ApplicantProfileResponse = dbQuery {
        val existing = ApplicantProfiles
            .selectAll()
            .where { ApplicantProfiles.userId eq userId }
            .singleOrNull()

        val now = Instant.now()
        val skillsText = request.skills.joinToString(",")

        if (existing == null) {
            val id = ApplicantProfiles.insertAndGetId {
                it[ApplicantProfiles.userId] = userId
                it[fullName] = request.fullName
                it[contacts] = request.contacts
                it[skills] = skillsText
                it[experience] = request.experience
                it[education] = request.education
                it[portfolioUrl] = request.portfolioUrl
                it[createdAt] = now
                it[updatedAt] = now
            }

            ApplicantProfiles
                .selectAll()
                .where { ApplicantProfiles.id eq id }
                .single()
                .toResponse()
        } else {
            ApplicantProfiles.update({ ApplicantProfiles.userId eq userId }) {
                it[fullName] = request.fullName
                it[contacts] = request.contacts
                it[skills] = skillsText
                it[experience] = request.experience
                it[education] = request.education
                it[portfolioUrl] = request.portfolioUrl
                it[updatedAt] = now
            }

            ApplicantProfiles
                .selectAll()
                .where { ApplicantProfiles.userId eq userId }
                .single()
                .toResponse()
        }
    }

    private fun ResultRow.toResponse(): ApplicantProfileResponse {
        return ApplicantProfileResponse(
            id = this[ApplicantProfiles.id].value.toString(),
            userId = this[ApplicantProfiles.userId].value.toString(),
            fullName = this[ApplicantProfiles.fullName],
            contacts = this[ApplicantProfiles.contacts],
            skills = this[ApplicantProfiles.skills]
                .split(",")
                .filter { it.isNotBlank() },
            experience = this[ApplicantProfiles.experience],
            education = this[ApplicantProfiles.education],
            portfolioUrl = this[ApplicantProfiles.portfolioUrl],
            createdAt = this[ApplicantProfiles.createdAt].toString(),
            updatedAt = this[ApplicantProfiles.updatedAt].toString()
        )
    }
}