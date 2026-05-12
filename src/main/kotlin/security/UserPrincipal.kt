package security

import domain.UserRole
import io.ktor.server.auth.Principal
import java.util.UUID

data class UserPrincipal(
    val userId: UUID,
    val role: UserRole
) : Principal