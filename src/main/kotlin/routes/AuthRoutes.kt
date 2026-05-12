package routes

import dto.LoginRequest
import dto.RefreshRequest
import dto.RegisterRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import services.AuthService
import domain.UserRole
import dto.LogoutRequest
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import security.requireRole

fun Route.authRoutes(authService: AuthService) {
    route("/api/auth") {

        post("/register") {
            val request = call.receive<RegisterRequest>()
            val response = authService.register(request)

            call.respond(
                status = HttpStatusCode.Created,
                message = response
            )
        }

        post("/login") {
            val request = call.receive<LoginRequest>()
            val response = authService.login(request)

            call.respond(response)
        }

        post("/refresh") {
            val request = call.receive<RefreshRequest>()
            val response = authService.refresh(request)

            call.respond(response)
        }
        post("/logout") {
            val request = call.receive<LogoutRequest>()
            authService.logout(request)

            call.respond(
                mapOf("message" to "Logged out")
            )
        }
        authenticate("auth-jwt") {
            post("/api/auth/revoke-all") {
                val principal = call.principal<security.UserPrincipal>()
                    ?: throw domain.AppException(
                        io.ktor.http.HttpStatusCode.Unauthorized,
                        "UNAUTHORIZED",
                        "Authentication required"
                    )

                authService.revokeAll(principal.userId)

                call.respond(
                    mapOf("message" to "All refresh tokens revoked")
                )
            }
        }
    }
}