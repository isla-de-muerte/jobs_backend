package repositories

import db.Categories
import db.dbQuery
import dto.CategoryResponse
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.selectAll

class CategoryRepository {

    suspend fun getAll(): List<CategoryResponse> = dbQuery {
        Categories
            .selectAll()
            .map { row -> row.toResponse() }
    }

    private fun ResultRow.toResponse(): CategoryResponse {
        return CategoryResponse(
            id = this[Categories.id].value.toString(),
            name = this[Categories.name],
            slug = this[Categories.slug]
        )
    }
}