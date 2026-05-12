package services

import domain.AppException
import domain.ApplicationStatus
import domain.UserRole
import dto.ApplyRequest
import dto.ApplicationResponse
import dto.EmployerApplicationResponse
import dto.UpdateApplicationStatusRequest
import io.ktor.http.HttpStatusCode
import repositories.ApplicationMessageRepository
import repositories.ApplicationRepository
import java.util.UUID

class ApplicationService(
    private val repository: ApplicationRepository,
    private val messageRepository: ApplicationMessageRepository,
    private val hiringResponsibilityService: HiringResponsibilityService
) {

    suspend fun apply(
        vacancyId: UUID,
        applicantId: UUID,
        request: ApplyRequest
    ): ApplicationResponse {
        return try {
            val response = repository.applyToVacancy(
                vacancyId = vacancyId,
                applicantId = applicantId,
                coverLetter = request.coverLetter
            )

            if (!request.coverLetter.isNullOrBlank()) {
                messageRepository.createMessage(
                    applicationId = UUID.fromString(response.id),
                    senderId = applicantId,
                    senderRole = UserRole.APPLICANT,
                    message = request.coverLetter
                )
            }

            hiringResponsibilityService.pauseVacancyIfOverloaded(vacancyId)

            response
        } catch (e: IllegalArgumentException) {
            throw AppException(
                status = HttpStatusCode.BadRequest,
                code = "APPLICATION_ERROR",
                message = e.message ?: "Cannot apply to vacancy"
            )
        }
    }

    suspend fun applicantApplications(
        applicantId: UUID
    ): List<ApplicationResponse> {
        return repository.findApplicantApplications(applicantId)
    }

    suspend fun employerApplicationsByVacancy(
        employerId: UUID,
        vacancyId: UUID
    ): List<EmployerApplicationResponse> {
        return try {
            repository.findEmployerApplicationsByVacancy(
                employerId = employerId,
                vacancyId = vacancyId
            )
        } catch (e: IllegalArgumentException) {
            throw AppException(
                status = HttpStatusCode.Forbidden,
                code = "ACCESS_DENIED",
                message = e.message ?: "Access denied"
            )
        }
    }

    suspend fun updateStatusByEmployer(
        employerId: UUID,
        applicationId: UUID,
        request: UpdateApplicationStatusRequest
    ): ApplicationResponse {
        return try {
            val response = repository.updateStatusByEmployer(
                employerId = employerId,
                applicationId = applicationId,
                status = ApplicationStatus.valueOf(request.status.name)
            )

            if (!request.message.isNullOrBlank()) {
                messageRepository.createMessage(
                    applicationId = applicationId,
                    senderId = employerId,
                    senderRole = UserRole.EMPLOYER,
                    message = request.message
                )
            }

            response
        } catch (e: IllegalArgumentException) {
            throw AppException(
                status = HttpStatusCode.Forbidden,
                code = "ACCESS_DENIED",
                message = e.message ?: "Application not found or access denied"
            )
        }
    }
}