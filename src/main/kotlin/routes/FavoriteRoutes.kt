package routes

import domain.UserRole
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import security.requireRole
import services.FavoriteService
import java.util.UUID

fun Route.favoriteRoutes(
    favoriteService: FavoriteService
) {
    authenticate("auth-jwt") {

        get("/api/me/favorites") {
            val principal = call.requireRole(UserRole.APPLICANT)

            call.respond(
                favoriteService.getFavorites(principal.userId)
            )
        }

        post("/api/vacancies/{vacancyId}/favorite") {
            val principal = call.requireRole(UserRole.APPLICANT)

            val vacancyId = UUID.fromString(
                call.parameters["vacancyId"]
            )

            call.respond(
                HttpStatusCode.Created,
                favoriteService.addFavorite(
                    applicantId = principal.userId,
                    vacancyId = vacancyId
                )
            )
        }

        delete("/api/vacancies/{vacancyId}/favorite") {
            val principal = call.requireRole(UserRole.APPLICANT)

            val vacancyId = UUID.fromString(
                call.parameters["vacancyId"]
            )

            favoriteService.removeFavorite(
                applicantId = principal.userId,
                vacancyId = vacancyId
            )

            call.respond(
                mapOf("message" to "Favorite removed")
            )
        }
    }
}