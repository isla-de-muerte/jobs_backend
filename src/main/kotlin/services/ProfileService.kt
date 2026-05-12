package services

import domain.AppException
import dto.ApplicantProfileRequest
import dto.ApplicantProfileResponse
import io.ktor.http.HttpStatusCode
import repositories.ProfileRepository
import java.util.UUID

class ProfileService(
    private val profileRepository: ProfileRepository
) {

    suspend fun getApplicantProfile(userId: UUID): ApplicantProfileResponse {
        return profileRepository.findApplicantProfile(userId)
            ?: throw AppException(
                status = HttpStatusCode.NotFound,
                code = "PROFILE_NOT_FOUND",
                message = "Applicant profile not found"
            )
    }

    suspend fun saveApplicantProfile(
        userId: UUID,
        request: ApplicantProfileRequest
    ): ApplicantProfileResponse {
        validate(request)
        return profileRepository.upsertApplicantProfile(userId, request)
    }

    private fun validate(request: ApplicantProfileRequest) {
        if (request.fullName.isBlank()) {
            throw AppException(HttpStatusCode.BadRequest, "INVALID_FULL_NAME", "Full name is required")
        }

        if (request.contacts.isBlank()) {
            throw AppException(HttpStatusCode.BadRequest, "INVALID_CONTACTS", "Contacts are required")
        }

        if (request.skills.isEmpty()) {
            throw AppException(HttpStatusCode.BadRequest, "INVALID_SKILLS", "At least one skill is required")
        }
    }
}