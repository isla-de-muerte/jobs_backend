package plugins

import domain.AppException
import dto.ErrorResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<AppException> { call, cause ->
            call.respond(
                cause.status,
                ErrorResponse(
                    status = cause.status.value,
                    code = cause.code,
                    message = cause.message
                )
            )
        }

        exception<IllegalArgumentException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(
                    status = 400,
                    code = "BAD_REQUEST",
                    message = cause.message ?: "Invalid request"
                )
            )
        }

        exception<Throwable> { call, cause ->
            call.application.log.error("Unhandled error", cause)

            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse(
                    status = 500,
                    code = "INTERNAL_ERROR",
                    message = "Unexpected server error"
                )
            )
        }
    }
}