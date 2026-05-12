package dto

import kotlinx.serialization.Serializable

@Serializable
data class FavoriteResponse(
    val id: String,
    val vacancyId: String,
    val createdAt: String
)