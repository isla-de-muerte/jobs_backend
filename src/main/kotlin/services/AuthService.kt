package services

import config.JwtConfig
import domain.AppException
import domain.UserRole
import dto.*
import io.ktor.http.HttpStatusCode
import repositories.RefreshTokenRepository
import repositories.UserModel
import repositories.UserRepository
import security.JwtService
import security.PasswordHasher
import java.security.MessageDigest
import java.time.Instant
import java.time.temporal.ChronoUnit

class AuthService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val passwordHasher: PasswordHasher,
    private val jwtService: JwtService,
    private val jwtConfig: JwtConfig
) {

    suspend fun register(request: RegisterRequest): AuthResponse {
        validateEmail(request.email)
        validatePassword(request.password)

        val existingUser = userRepository.findByEmail(request.email)
        if (existingUser != null) {
            throw AppException(
                status = HttpStatusCode.Conflict,
                code = "EMAIL_ALREADY_EXISTS",
                message = "User with this email already exists"
            )
        }

        val role = request.role.toDomain()
        val passwordHash = passwordHasher.hash(request.password)

        val user = userRepository.create(
            email = request.email,
            passwordHash = passwordHash,
            role = role
        )

        return issueTokens(user)
    }

    suspend fun login(request: LoginRequest): AuthResponse {
        validateEmail(request.email)

        val user = userRepository.findByEmail(request.email)
            ?: throw invalidCredentials()

        val passwordValid = passwordHasher.verify(
            hash = user.passwordHash,
            password = request.password
        )

        if (!passwordValid) {
            throw invalidCredentials()
        }

        return issueTokens(user)
    }

    suspend fun refresh(request: RefreshRequest): AuthResponse {
        val decoded = try {
            jwtService.refreshVerifier().verify(request.refreshToken)
        } catch (_: Exception) {
            throw AppException(
                status = HttpStatusCode.Unauthorized,
                code = "INVALID_REFRESH_TOKEN",
                message = "Refresh token is invalid"
            )
        }

        val tokenHash = sha256(request.refreshToken)

        val storedToken = refreshTokenRepository.findActiveByHash(tokenHash)
            ?: throw AppException(
                status = HttpStatusCode.Unauthorized,
                code = "INVALID_REFRESH_TOKEN",
                message = "Refresh token is invalid or expired"
            )

        refreshTokenRepository.revoke(storedToken.id)

        val user = userRepository.findById(storedToken.userId)
            ?: throw AppException(
                status = HttpStatusCode.Unauthorized,
                code = "USER_NOT_FOUND",
                message = "User not found"
            )

        return issueTokens(user)
    }

    private suspend fun issueTokens(user: UserModel): AuthResponse {
        val accessToken = jwtService.createAccessToken(
            userId = user.id,
            role = user.role
        )

        val refreshToken = jwtService.createRefreshToken(
            userId = user.id
        )

        refreshTokenRepository.save(
            userId = user.id,
            tokenHash = sha256(refreshToken),
            expiresAt = Instant.now().plus(jwtConfig.refreshTtlDays, ChronoUnit.DAYS)
        )

        return AuthResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            user = UserResponse(
                id = user.id.toString(),
                email = user.email,
                role = user.role.toDto()
            )
        )
    }

    private fun validateEmail(email: String) {
        if (!email.contains("@") || email.length < 5) {
            throw AppException(
                status = HttpStatusCode.BadRequest,
                code = "INVALID_EMAIL",
                message = "Email is invalid"
            )
        }
    }

    private fun validatePassword(password: String) {
        if (password.length < 8) {
            throw AppException(
                status = HttpStatusCode.BadRequest,
                code = "INVALID_PASSWORD",
                message = "Password must contain at least 8 characters"
            )
        }
    }

    private fun invalidCredentials(): AppException {
        return AppException(
            status = HttpStatusCode.Unauthorized,
            code = "INVALID_CREDENTIALS",
            message = "Invalid email or password"
        )
    }

    private fun UserRoleDto.toDomain(): UserRole {
        return when (this) {
            UserRoleDto.APPLICANT -> UserRole.APPLICANT
            UserRoleDto.EMPLOYER -> UserRole.EMPLOYER
        }
    }

    private fun UserRole.toDto(): UserRoleDto {
        return when (this) {
            UserRole.APPLICANT -> UserRoleDto.APPLICANT
            UserRole.EMPLOYER -> UserRoleDto.EMPLOYER
        }
    }

    private fun sha256(value: String): String {
        val bytes = MessageDigest
            .getInstance("SHA-256")
            .digest(value.toByteArray())

        return bytes.joinToString("") { "%02x".format(it) }
    }
    suspend fun logout(request: LogoutRequest) {
        refreshTokenRepository.revokeByHash(
            sha256(request.refreshToken)
        )
    }

    suspend fun revokeAll(userId: java.util.UUID) {
        refreshTokenRepository.revokeAllByUserId(userId)
    }
}