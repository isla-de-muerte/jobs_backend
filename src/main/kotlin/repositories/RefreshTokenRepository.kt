package repositories

import db.RefreshTokens
import db.dbQuery
import org.jetbrains.exposed.sql.*
import java.time.Instant
import java.util.UUID

data class RefreshTokenModel(
    val id: UUID,
    val userId: UUID,
    val tokenHash: String,
    val revoked: Boolean,
    val expiresAt: Instant
)

class RefreshTokenRepository {

    suspend fun save(
        userId: UUID,
        tokenHash: String,
        expiresAt: Instant
    ): RefreshTokenModel = dbQuery {
        val id = RefreshTokens.insertAndGetId {
            it[RefreshTokens.userId] = userId
            it[RefreshTokens.tokenHash] = tokenHash
            it[RefreshTokens.expiresAt] = expiresAt
            it[revoked] = false
        }.value

        RefreshTokenModel(
            id = id,
            userId = userId,
            tokenHash = tokenHash,
            revoked = false,
            expiresAt = expiresAt
        )
    }

    suspend fun findActiveByHash(tokenHash: String): RefreshTokenModel? = dbQuery {
        RefreshTokens
            .selectAll()
            .where {
                (RefreshTokens.tokenHash eq tokenHash) and
                        (RefreshTokens.revoked eq false) and
                        (RefreshTokens.expiresAt greater Instant.now())
            }
            .singleOrNull()
            ?.toRefreshTokenModel()
    }

    suspend fun revoke(id: UUID) = dbQuery {
        RefreshTokens.update({ RefreshTokens.id eq id }) {
            it[revoked] = true
        }
        Unit
    }

    private fun ResultRow.toRefreshTokenModel(): RefreshTokenModel {
        return RefreshTokenModel(
            id = this[RefreshTokens.id].value,
            userId = this[RefreshTokens.userId].value,
            tokenHash = this[RefreshTokens.tokenHash],
            revoked = this[RefreshTokens.revoked],
            expiresAt = this[RefreshTokens.expiresAt]
        )
    }
    suspend fun revokeByHash(tokenHash: String) = dbQuery {
        RefreshTokens.update({ RefreshTokens.tokenHash eq tokenHash }) {
            it[revoked] = true
        }
        Unit
    }

    suspend fun revokeAllByUserId(userId: UUID) = dbQuery {
        RefreshTokens.update({ RefreshTokens.userId eq userId }) {
            it[revoked] = true
        }
        Unit
    }
}