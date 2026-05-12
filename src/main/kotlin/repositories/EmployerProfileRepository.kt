package repositories

import db.EmployerProfiles
import db.dbQuery
import dto.EmployerProfileRequest
import dto.EmployerProfileResponse
import org.jetbrains.exposed.sql.*
import java.time.Instant
import java.util.UUID

class EmployerProfileRepository {

    suspend fun findByUserId(
        userId: UUID
    ): EmployerProfileResponse? = dbQuery {
        EmployerProfiles
            .selectAll()
            .where { EmployerProfiles.userId eq userId }
            .singleOrNull()
            ?.toResponse()
    }

    suspend fun upsert(
        userId: UUID,
        request: EmployerProfileRequest
    ): EmployerProfileResponse = dbQuery {
        val existing = EmployerProfiles
            .selectAll()
            .where { EmployerProfiles.userId eq userId }
            .singleOrNull()

        val now = Instant.now()

        if (existing == null) {
            val id = EmployerProfiles.insertAndGetId {
                it[EmployerProfiles.userId] = userId
                it[companyName] = request.companyName
                it[description] = request.description
                it[website] = request.website
                it[createdAt] = now
                it[updatedAt] = now
            }

            EmployerProfiles
                .selectAll()
                .where { EmployerProfiles.id eq id }
                .single()
                .toResponse()
        } else {
            EmployerProfiles.update({ EmployerProfiles.userId eq userId }) {
                it[companyName] = request.companyName
                it[description] = request.description
                it[website] = request.website
                it[updatedAt] = now
            }

            EmployerProfiles
                .selectAll()
                .where { EmployerProfiles.userId eq userId }
                .single()
                .toResponse()
        }
    }

    private fun ResultRow.toResponse(): EmployerProfileResponse {
        return EmployerProfileResponse(
            id = this[EmployerProfiles.id].value.toString(),
            userId = this[EmployerProfiles.userId].value.toString(),
            companyName = this[EmployerProfiles.companyName],
            description = this[EmployerProfiles.description],
            website = this[EmployerProfiles.website],
            createdAt = this[EmployerProfiles.createdAt].toString(),
            updatedAt = this[EmployerProfiles.updatedAt].toString()
        )
    }
}