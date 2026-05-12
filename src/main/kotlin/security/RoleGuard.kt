package security

import domain.AppException
import domain.UserRole
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.principal

fun ApplicationCall.requireRole(role: UserRole): UserPrincipal {
    val principal = principal<UserPrincipal>()
        ?: throw AppException(
            status = HttpStatusCode.Unauthorized,
            code = "UNAUTHORIZED",
            message = "Authentication required"
        )

    if (principal.role != role) {
        throw AppException(
            status = HttpStatusCode.Forbidden,
            code = "FORBIDDEN",
            message = "Access denied for this role"
        )
    }

    return principal
}