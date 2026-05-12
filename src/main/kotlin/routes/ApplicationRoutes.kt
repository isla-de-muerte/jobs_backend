package routes

import domain.UserRole
import dto.ApplyRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import security.requireRole
import services.ApplicationService
import java.util.UUID
import dto.UpdateApplicationStatusRequest
fun Route.applicationRoutes(
    applicationService: ApplicationService
) {
    authenticate("auth-jwt") {

        post("/api/vacancies/{vacancyId}/applications") {
            val principal = call.requireRole(UserRole.APPLICANT)

            val vacancyId = UUID.fromString(
                call.parameters["vacancyId"]
            )

            val request = call.receive<ApplyRequest>()

            val response = applicationService.apply(
                vacancyId = vacancyId,
                applicantId = principal.userId,
                request = request
            )

            call.respond(HttpStatusCode.Created, response)
        }

        get("/api/me/applications") {
            val principal = call.requireRole(UserRole.APPLICANT)

            call.respond(
                applicationService.applicantApplications(
                    applicantId = principal.userId
                )
            )
        }

        get("/api/employer/vacancies/{vacancyId}/applications") {
            val principal = call.requireRole(UserRole.EMPLOYER)

            val vacancyId = UUID.fromString(
                call.parameters["vacancyId"]
            )

            call.respond(
                applicationService.employerApplicationsByVacancy(
                    employerId = principal.userId,
                    vacancyId = vacancyId
                )
            )
        }
        patch("/api/employer/applications/{applicationId}/status") {
            val principal = call.requireRole(UserRole.EMPLOYER)

            val applicationId = UUID.fromString(
                call.parameters["applicationId"]
            )

            val request = call.receive<UpdateApplicationStatusRequest>()

            val response = applicationService.updateStatusByEmployer(
                employerId = principal.userId,
                applicationId = applicationId,
                request = request
            )

            call.respond(response)
        }
    }
}