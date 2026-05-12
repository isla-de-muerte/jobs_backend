package dto

import kotlinx.serialization.Serializable

@Serializable
data class ApplyRequest(
    val coverLetter: String? = null
)

@Serializable
data class ApplicationResponse(
    val id: String,
    val vacancyId: String,
    val applicantId: String,
    val profileId: String,
    val coverLetter: String?,
    val status: String,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class EmployerApplicationResponse(
    val id: String,
    val vacancyId: String,
    val applicantId: String,
    val coverLetter: String?,
    val status: String,
    val createdAt: String,
    val resume: ApplicantProfileResponse
)

@Serializable
enum class ApplicationStatusDto {
    NEW,
    VIEWED,
    INTERVIEW,
    REJECTED,
    ACCEPTED
}

@Serializable
data class UpdateApplicationStatusRequest(
    val status: ApplicationStatusDto,
    val message: String? = null
)