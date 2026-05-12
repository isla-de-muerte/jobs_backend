package dto

import kotlinx.serialization.Serializable

@Serializable
data class ApplicantProfileRequest(
    val fullName: String,
    val contacts: String,
    val skills: List<String>,
    val experience: String? = null,
    val education: String? = null,
    val portfolioUrl: String? = null
)

@Serializable
data class ApplicantProfileResponse(
    val id: String,
    val userId: String,
    val fullName: String,
    val contacts: String,
    val skills: List<String>,
    val experience: String?,
    val education: String?,
    val portfolioUrl: String?,
    val createdAt: String,
    val updatedAt: String
)