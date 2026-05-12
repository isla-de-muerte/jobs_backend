package plugins

import config.JwtConfig
import domain.UserRole
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.jwt
import security.JwtService
import security.UserPrincipal
import java.util.UUID

fun Application.configureAuth(config: JwtConfig) {
    val jwtService = JwtService(config)

    install(Authentication) {
        jwt("auth-jwt") {
            realm = config.realm
            verifier(jwtService.accessVerifier())

            validate { credential ->
                val userId = credential.subject
                val role = credential.payload.getClaim("role").asString()

                if (userId != null && role != null) {
                    UserPrincipal(
                        userId = UUID.fromString(userId),
                        role = UserRole.valueOf(role)
                    )
                } else {
                    null
                }
            }
        }
    }
}