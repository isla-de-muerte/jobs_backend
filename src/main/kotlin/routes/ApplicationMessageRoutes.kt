package routes

import dto.SendApplicationMessageRequest
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import security.UserPrincipal
import services.ApplicationMessageService
import java.util.UUID

fun Route.applicationMessageRoutes(
    applicationMessageService: ApplicationMessageService
) {
    authenticate("auth-jwt") {

        get("/api/applications/unread-counts") {
            val principal = call.principal<UserPrincipal>()
                ?: error("Authentication required")

            call.respond(
                applicationMessageService.getUnreadCounts(
                    userId = principal.userId,
                    role = principal.role
                )
            )
        }

        route("/api/applications/{applicationId}/messages") {

            get {
                val principal = call.principal<UserPrincipal>()
                    ?: error("Authentication required")

                val applicationId = UUID.fromString(
                    call.parameters["applicationId"]
                )

                call.respond(
                    applicationMessageService.getMessages(
                        applicationId = applicationId,
                        userId = principal.userId,
                        role = principal.role
                    )
                )
            }

            post {
                val principal = call.principal<UserPrincipal>()
                    ?: error("Authentication required")

                val applicationId = UUID.fromString(
                    call.parameters["applicationId"]
                )

                val request = call.receive<SendApplicationMessageRequest>()

                call.respond(
                    applicationMessageService.sendMessage(
                        applicationId = applicationId,
                        userId = principal.userId,
                        role = principal.role,
                        request = request
                    )
                )
            }
        }

        post("/api/applications/{applicationId}/read") {
            val principal = call.principal<UserPrincipal>()
                ?: error("Authentication required")

            val applicationId = UUID.fromString(
                call.parameters["applicationId"]
            )

            applicationMessageService.markAsRead(
                applicationId = applicationId,
                userId = principal.userId,
                role = principal.role
            )

            call.respond(
                mapOf("message" to "Chat marked as read")
            )
        }
    }
}