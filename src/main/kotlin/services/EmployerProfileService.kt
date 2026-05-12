package services

import domain.AppException
import dto.EmployerProfileRequest
import dto.EmployerProfileResponse
import io.ktor.http.HttpStatusCode
import repositories.EmployerProfileRepository
import java.util.UUID

class EmployerProfileService(
    private val repository: EmployerProfileRepository
) {

    suspend fun getProfile(
        userId: UUID
    ): EmployerProfileResponse {
        return repository.findByUserId(userId)
            ?: throw AppException(
                status = HttpStatusCode.NotFound,
                code = "EMPLOYER_PROFILE_NOT_FOUND",
                message = "Employer profile not found"
            )
    }

    suspend fun saveProfile(
        userId: UUID,
        request: EmployerProfileRequest
    ): EmployerProfileResponse {
        validate(request)

        return repository.upsert(
            userId = userId,
            request = request
        )
    }

    private fun validate(request: EmployerProfileRequest) {
        if (request.companyName.isBlank()) {
            throw AppException(
                status = HttpStatusCode.BadRequest,
                code = "INVALID_COMPANY_NAME",
                message = "Company name is required"
            )
        }

        if (request.companyName.length < 2) {
            throw AppException(
                status = HttpStatusCode.BadRequest,
                code = "INVALID_COMPANY_NAME",
                message = "Company name must contain at least 2 characters"
            )
        }
    }
}