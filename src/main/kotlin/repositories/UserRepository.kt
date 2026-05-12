package repositories

import db.Users
import db.dbQuery
import domain.UserRole
import org.jetbrains.exposed.sql.*
import java.time.Instant
import java.util.UUID

data class UserModel(
    val id: UUID,
    val email: String,
    val passwordHash: String,
    val role: UserRole
)

class UserRepository {

    suspend fun create(
        email: String,
        passwordHash: String,
        role: UserRole
    ): UserModel = dbQuery {
        val now = Instant.now()

        val id = Users.insertAndGetId {
            it[Users.email] = email.lowercase()
            it[Users.passwordHash] = passwordHash
            it[Users.role] = role
            it[createdAt] = now
            it[updatedAt] = now
        }.value

        UserModel(
            id = id,
            email = email.lowercase(),
            passwordHash = passwordHash,
            role = role
        )
    }

    suspend fun findByEmail(email: String): UserModel? = dbQuery {
        Users
            .selectAll()
            .where { Users.email eq email.lowercase() }
            .singleOrNull()
            ?.toUserModel()
    }

    suspend fun findById(id: UUID): UserModel? = dbQuery {
        Users
            .selectAll()
            .where { Users.id eq id }
            .singleOrNull()
            ?.toUserModel()
    }

    private fun ResultRow.toUserModel(): UserModel {
        return UserModel(
            id = this[Users.id].value,
            email = this[Users.email],
            passwordHash = this[Users.passwordHash],
            role = this[Users.role]
        )
    }
}