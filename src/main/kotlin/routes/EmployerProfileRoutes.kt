package routes

import domain.UserRole
import dto.EmployerProfileRequest
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import security.requireRole
import services.EmployerProfileService

fun Route.employerProfileRoutes(
    employerProfileService: EmployerProfileService
) {
    authenticate("auth-jwt") {
        route("/api/employer/profile") {

            get {
                val principal = call.requireRole(UserRole.EMPLOYER)

                call.respond(
                    employerProfileService.getProfile(
                        userId = principal.userId
                    )
                )
            }

            put {
                val principal = call.requireRole(UserRole.EMPLOYER)
                val request = call.receive<EmployerProfileRequest>()

                call.respond(
                    employerProfileService.saveProfile(
                        userId = principal.userId,
                        request = request
                    )
                )
            }
        }
    }
}