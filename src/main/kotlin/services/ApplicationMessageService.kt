package services

import domain.AppException
import domain.UserRole
import dto.ApplicationMessageResponse
import dto.ApplicationUnreadCountResponse
import dto.SendApplicationMessageRequest
import io.ktor.http.HttpStatusCode
import repositories.ApplicationMessageRepository
import java.util.UUID

class ApplicationMessageService(
    private val repository: ApplicationMessageRepository
) {

    suspend fun getMessages(
        applicationId: UUID,
        userId: UUID,
        role: UserRole
    ): List<ApplicationMessageResponse> {
        ensureAccess(applicationId, userId, role)

        return repository.getMessages(applicationId)
    }

    suspend fun sendMessage(
        applicationId: UUID,
        userId: UUID,
        role: UserRole,
        request: SendApplicationMessageRequest
    ): ApplicationMessageResponse {
        if (request.message.isBlank()) {
            throw AppException(
                status = HttpStatusCode.BadRequest,
                code = "EMPTY_MESSAGE",
                message = "Message cannot be empty"
            )
        }

        ensureAccess(applicationId, userId, role)

        return repository.createMessage(
            applicationId = applicationId,
            senderId = userId,
            senderRole = role,
            message = request.message
        )
    }

    suspend fun markAsRead(
        applicationId: UUID,
        userId: UUID,
        role: UserRole
    ) {
        ensureAccess(applicationId, userId, role)

        repository.markAsRead(
            applicationId = applicationId,
            userId = userId
        )
    }

    suspend fun getUnreadCounts(
        userId: UUID,
        role: UserRole
    ): List<ApplicationUnreadCountResponse> {
        return repository.getUnreadCounts(
            userId = userId,
            role = role
        )
    }

    private suspend fun ensureAccess(
        applicationId: UUID,
        userId: UUID,
        role: UserRole
    ) {
        val hasAccess = repository.hasAccessToApplication(
            applicationId = applicationId,
            userId = userId,
            role = role
        )

        if (!hasAccess) {
            throw AppException(
                status = HttpStatusCode.Forbidden,
                code = "APPLICATION_CHAT_ACCESS_DENIED",
                message = "You do not have access to this application chat"
            )
        }
    }
}