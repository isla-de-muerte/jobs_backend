package services

import dto.CategoryResponse
import repositories.CategoryRepository

class CategoryService(
    private val repository: CategoryRepository
) {

    suspend fun getAll(): List<CategoryResponse> {
        return repository.getAll()
    }
}