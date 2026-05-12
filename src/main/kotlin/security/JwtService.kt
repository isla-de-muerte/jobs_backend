package security

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import config.JwtConfig
import domain.UserRole
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.UUID

class JwtService(
    private val config: JwtConfig
) {
    fun createAccessToken(userId: UUID, role: UserRole): String {
        val expiresAt = Instant.now().plus(config.accessTtlMinutes, ChronoUnit.MINUTES)

        return JWT.create()
            .withIssuer(config.issuer)
            .withAudience(config.audience)
            .withSubject(userId.toString())
            .withClaim("role", role.name)
            .withExpiresAt(Date.from(expiresAt))
            .sign(Algorithm.HMAC256(config.accessSecret))
    }

    fun createRefreshToken(userId: UUID): String {
        val expiresAt = Instant.now().plus(config.refreshTtlDays, ChronoUnit.DAYS)

        return JWT.create()
            .withIssuer(config.issuer)
            .withAudience(config.audience)
            .withSubject(userId.toString())
            .withExpiresAt(Date.from(expiresAt))
            .sign(Algorithm.HMAC256(config.refreshSecret))
    }

    fun accessVerifier(): JWTVerifier {
        return JWT.require(Algorithm.HMAC256(config.accessSecret))
            .withIssuer(config.issuer)
            .withAudience(config.audience)
            .build()
    }

    fun refreshVerifier(): JWTVerifier {
        return JWT.require(Algorithm.HMAC256(config.refreshSecret))
            .withIssuer(config.issuer)
            .withAudience(config.audience)
            .build()
    }
}