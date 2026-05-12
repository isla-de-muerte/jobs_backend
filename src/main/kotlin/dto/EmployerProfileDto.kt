package dto

import kotlinx.serialization.Serializable

@Serializable
data class EmployerProfileRequest(
    val companyName: String,
    val description: String? = null,
    val website: String? = null
)

@Serializable
data class EmployerProfileResponse(
    val id: String,
    val userId: String,
    val companyName: String,
    val description: String?,
    val website: String?,
    val createdAt: String,
    val updatedAt: String
)