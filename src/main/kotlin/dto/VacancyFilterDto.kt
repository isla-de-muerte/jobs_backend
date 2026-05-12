package dto

import kotlinx.serialization.Serializable

@Serializable
data class VacancyFilterParams(
    val page: Int = 1,
    val size: Int = 10,
    val categoryId: String? = null,
    val workFormat: WorkFormatDto? = null,
    val sort: String = "createdAt_desc",
    val query: String? = null
)