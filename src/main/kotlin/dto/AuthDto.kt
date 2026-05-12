package dto

import kotlinx.serialization.Serializable

@Serializable
enum class UserRoleDto {
    APPLICANT,
    EMPLOYER
}

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val role: UserRoleDto
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class RefreshRequest(
    val refreshToken: String
)

@Serializable
data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val user: UserResponse
)

@Serializable
data class UserResponse(
    val id: String,
    val email: String,
    val role: UserRoleDto
)
@Serializable
data class LogoutRequest(
    val refreshToken: String
)