package repositories

import db.ApplicationChatReads
import db.ApplicationMessages
import db.Applications
import db.Vacancies
import db.dbQuery
import domain.UserRole
import dto.ApplicationMessageResponse
import dto.ApplicationUnreadCountResponse
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq
import java.time.Instant
import java.util.UUID

class ApplicationMessageRepository {

    suspend fun hasAccessToApplication(
        applicationId: UUID,
        userId: UUID,
        role: UserRole
    ): Boolean = dbQuery {
        when (role) {
            UserRole.APPLICANT -> {
                Applications
                    .selectAll()
                    .where {
                        (Applications.id eq applicationId) and
                                (Applications.applicantId eq userId)
                    }
                    .singleOrNull() != null
            }

            UserRole.EMPLOYER -> {
                Applications
                    .innerJoin(Vacancies)
                    .selectAll()
                    .where {
                        (Applications.id eq applicationId) and
                                (Vacancies.employerId eq userId)
                    }
                    .singleOrNull() != null
            }
        }
    }

    suspend fun createMessage(
        applicationId: UUID,
        senderId: UUID,
        senderRole: UserRole,
        message: String
    ): ApplicationMessageResponse = dbQuery {
        val now = Instant.now()

        val id = ApplicationMessages.insertAndGetId {
            it[ApplicationMessages.applicationId] = applicationId
            it[ApplicationMessages.senderId] = senderId
            it[ApplicationMessages.senderRole] = senderRole
            it[ApplicationMessages.message] = message.trim()
            it[ApplicationMessages.createdAt] = now
        }

        ApplicationMessages
            .selectAll()
            .where { ApplicationMessages.id eq id }
            .single()
            .toResponse()
    }

    suspend fun getMessages(
        applicationId: UUID
    ): List<ApplicationMessageResponse> = dbQuery {
        ApplicationMessages
            .selectAll()
            .where { ApplicationMessages.applicationId eq applicationId }
            .orderBy(ApplicationMessages.createdAt to SortOrder.ASC)
            .map { row -> row.toResponse() }
    }

    suspend fun markAsRead(
        applicationId: UUID,
        userId: UUID
    ) = dbQuery {
        val now = Instant.now()

        val existing = ApplicationChatReads
            .selectAll()
            .where {
                (ApplicationChatReads.applicationId eq applicationId) and
                        (ApplicationChatReads.userId eq userId)
            }
            .singleOrNull()

        if (existing == null) {
            ApplicationChatReads.insert {
                it[ApplicationChatReads.applicationId] = applicationId
                it[ApplicationChatReads.userId] = userId
                it[ApplicationChatReads.lastReadAt] = now
                it[ApplicationChatReads.createdAt] = now
                it[ApplicationChatReads.updatedAt] = now
            }
        } else {
            ApplicationChatReads.update({
                (ApplicationChatReads.applicationId eq applicationId) and
                        (ApplicationChatReads.userId eq userId)
            }) {
                it[ApplicationChatReads.lastReadAt] = now
                it[ApplicationChatReads.updatedAt] = now
            }
        }

        Unit
    }

    suspend fun getUnreadCounts(
        userId: UUID,
        role: UserRole
    ): List<ApplicationUnreadCountResponse> = dbQuery {
        val applicationIds = when (role) {
            UserRole.APPLICANT -> {
                Applications
                    .selectAll()
                    .where { Applications.applicantId eq userId }
                    .map { row -> row[Applications.id].value }
            }

            UserRole.EMPLOYER -> {
                Applications
                    .innerJoin(Vacancies)
                    .selectAll()
                    .where { Vacancies.employerId eq userId }
                    .map { row -> row[Applications.id].value }
            }
        }

        applicationIds.map { applicationId ->
            val readRow = ApplicationChatReads
                .selectAll()
                .where {
                    (ApplicationChatReads.applicationId eq applicationId) and
                            (ApplicationChatReads.userId eq userId)
                }
                .singleOrNull()

            val lastReadAt = readRow?.get(ApplicationChatReads.lastReadAt)

            val unreadCount = if (lastReadAt == null) {
                ApplicationMessages
                    .selectAll()
                    .where {
                        (ApplicationMessages.applicationId eq applicationId) and
                                (ApplicationMessages.senderId neq userId)
                    }
                    .count()
            } else {
                ApplicationMessages
                    .selectAll()
                    .where {
                        (ApplicationMessages.applicationId eq applicationId) and
                                (ApplicationMessages.senderId neq userId) and
                                (ApplicationMessages.createdAt greater lastReadAt)
                    }
                    .count()
            }

            ApplicationUnreadCountResponse(
                applicationId = applicationId.toString(),
                unreadCount = unreadCount.toInt()
            )
        }
    }

    private fun ResultRow.toResponse(): ApplicationMessageResponse {
        return ApplicationMessageResponse(
            id = this[ApplicationMessages.id].value.toString(),
            applicationId = this[ApplicationMessages.applicationId].value.toString(),
            senderId = this[ApplicationMessages.senderId].value.toString(),
            senderRole = this[ApplicationMessages.senderRole].name,
            message = this[ApplicationMessages.message],
            createdAt = this[ApplicationMessages.createdAt].toString()
        )
    }
}