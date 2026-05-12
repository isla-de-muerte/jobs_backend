package routes

import domain.UserRole
import dto.VacancyCreateRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import security.requireRole
import services.VacancyService
import java.util.UUID

fun Route.employerVacancyRoutes(
    vacancyService: VacancyService
) {
    authenticate("auth-jwt") {
        route("/api/employer/vacancies") {

            post {
                val principal = call.requireRole(UserRole.EMPLOYER)
                val request = call.receive<VacancyCreateRequest>()

                val response = vacancyService.create(
                    employerId = principal.userId,
                    request = request
                )

                call.respond(
                    status = HttpStatusCode.Created,
                    message = response
                )
            }

            get {
                val principal = call.requireRole(UserRole.EMPLOYER)

                val vacancies = vacancyService.employerVacancies(
                    employerId = principal.userId
                )

                call.respond(vacancies)
            }

            post("/{id}/publish") {
                val principal = call.requireRole(UserRole.EMPLOYER)

                val id = UUID.fromString(
                    call.parameters["id"]
                )

                vacancyService.publish(
                    employerId = principal.userId,
                    id = id
                )

                call.respond(
                    mapOf("message" to "Vacancy published")
                )
            }

            post("/{id}/resume") {
                val principal = call.requireRole(UserRole.EMPLOYER)

                val id = UUID.fromString(
                    call.parameters["id"]
                )

                vacancyService.resumeAfterOverload(
                    employerId = principal.userId,
                    id = id
                )

                call.respond(
                    mapOf("message" to "Vacancy resumed")
                )
            }

            post("/{id}/archive") {
                call.requireRole(UserRole.EMPLOYER)

                val id = UUID.fromString(
                    call.parameters["id"]
                )

                vacancyService.archive(id)

                call.respond(
                    mapOf("message" to "Vacancy archived")
                )
            }
        }
    }
}