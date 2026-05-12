package services

import domain.AppException
import dto.VacancyCreateRequest
import dto.VacancyFilterParams
import dto.VacancyListResponse
import dto.VacancyResponse
import io.ktor.http.HttpStatusCode
import repositories.VacancyRepository
import java.util.UUID

class VacancyService(
    private val repository: VacancyRepository,
    private val hiringResponsibilityService: HiringResponsibilityService
) {

    suspend fun create(
        employerId: UUID,
        request: VacancyCreateRequest
    ): VacancyResponse {
        validate(request)

        return repository.create(
            employerId = employerId,
            request = request
        )
    }

    suspend fun employerVacancies(
        employerId: UUID
    ): List<VacancyResponse> {
        hiringResponsibilityService.archiveAbandonedPausedVacancies()

        return repository.findEmployerVacancies(
            employerId = employerId
        )
    }

    suspend fun publish(
        employerId: UUID,
        id: UUID
    ) {
        hiringResponsibilityService.archiveAbandonedPausedVacancies()
        hiringResponsibilityService.ensureEmployerCanPublish(employerId)
        repository.publish(id)
    }

    suspend fun resumeAfterOverload(
        employerId: UUID,
        id: UUID
    ) {
        val vacancy = repository.findById(id)
            ?: throw AppException(
                status = HttpStatusCode.NotFound,
                code = "VACANCY_NOT_FOUND",
                message = "Vacancy not found"
            )

        if (vacancy.employerId != employerId.toString()) {
            throw AppException(
                status = HttpStatusCode.Forbidden,
                code = "ACCESS_DENIED",
                message = "You do not own this vacancy"
            )
        }

        hiringResponsibilityService.ensureVacancyCanBeResumed(id)
        repository.resumeAfterOverload(id)
    }

    suspend fun archive(
        id: UUID
    ) {
        repository.archive(id)
    }

    suspend fun publicVacancies(
        params: VacancyFilterParams
    ): VacancyListResponse {
        val (items, total) = repository.publicVacancies(params)

        return VacancyListResponse(
            items = items,
            page = params.page,
            size = params.size,
            total = total
        )
    }

    suspend fun vacancyById(
        id: UUID
    ): VacancyResponse {
        repository.incrementViews(id)

        return repository.findById(id)
            ?: throw AppException(
                status = HttpStatusCode.NotFound,
                code = "VACANCY_NOT_FOUND",
                message = "Vacancy not found"
            )
    }

    private fun validate(request: VacancyCreateRequest) {
        if (request.title.isBlank() || request.title.length < 3) {
            throw AppException(
                status = HttpStatusCode.BadRequest,
                code = "INVALID_TITLE",
                message = "Title must contain at least 3 characters"
            )
        }

        if (request.description.isBlank() || request.description.length < 10) {
            throw AppException(
                status = HttpStatusCode.BadRequest,
                code = "INVALID_DESCRIPTION",
                message = "Description must contain at least 10 characters"
            )
        }

        if (request.salaryFrom != null && request.salaryFrom < 0) {
            throw AppException(
                status = HttpStatusCode.BadRequest,
                code = "INVALID_SALARY",
                message = "Salary from cannot be negative"
            )
        }

        if (request.salaryTo != null && request.salaryTo < 0) {
            throw AppException(
                status = HttpStatusCode.BadRequest,
                code = "INVALID_SALARY",
                message = "Salary to cannot be negative"
            )
        }

        if (
            request.salaryFrom != null &&
            request.salaryTo != null &&
            request.salaryFrom > request.salaryTo
        ) {
            throw AppException(
                status = HttpStatusCode.BadRequest,
                code = "INVALID_SALARY_RANGE",
                message = "Salary from cannot be greater than salary to"
            )
        }
    }
}