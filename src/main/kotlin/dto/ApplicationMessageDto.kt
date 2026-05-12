package dto

import kotlinx.serialization.Serializable

@Serializable
data class SendApplicationMessageRequest(
    val message: String
)

@Serializable
data class ApplicationMessageResponse(
    val id: String,
    val applicationId: String,
    val senderId: String,
    val senderRole: String,
    val message: String,
    val createdAt: String
)

@Serializable
data class ApplicationUnreadCountResponse(
    val applicationId: String,
    val unreadCount: Int
)