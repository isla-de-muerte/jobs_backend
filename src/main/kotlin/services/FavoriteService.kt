package services

import domain.AppException
import dto.FavoriteResponse
import dto.VacancyResponse
import io.ktor.http.HttpStatusCode
import repositories.FavoriteRepository
import java.util.UUID

class FavoriteService(
    private val repository: FavoriteRepository
) {

    suspend fun addFavorite(
        applicantId: UUID,
        vacancyId: UUID
    ): FavoriteResponse {
        return try {
            repository.addFavorite(applicantId, vacancyId)
        } catch (e: IllegalArgumentException) {
            throw AppException(
                HttpStatusCode.BadRequest,
                "FAVORITE_ERROR",
                e.message ?: "Cannot add favorite"
            )
        }
    }

    suspend fun removeFavorite(
        applicantId: UUID,
        vacancyId: UUID
    ) {
        repository.removeFavorite(applicantId, vacancyId)
    }

    suspend fun getFavorites(
        applicantId: UUID
    ): List<VacancyResponse> {
        return repository.getFavorites(applicantId)
    }
}