package routes

import domain.UserRole
import dto.ApplicantProfileRequest
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import security.requireRole
import services.ProfileService

fun Route.profileRoutes(profileService: ProfileService) {
    authenticate("auth-jwt") {
        route("/api/me/profile") {

            get {
                val principal = call.requireRole(UserRole.APPLICANT)
                call.respond(profileService.getApplicantProfile(principal.userId))
            }

            put {
                val principal = call.requireRole(UserRole.APPLICANT)
                val request = call.receive<ApplicantProfileRequest>()
                call.respond(profileService.saveApplicantProfile(principal.userId, request))
            }
        }
    }
}