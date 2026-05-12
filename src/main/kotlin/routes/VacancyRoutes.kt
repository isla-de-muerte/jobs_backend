package routes

import dto.VacancyFilterParams
import dto.WorkFormatDto
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import services.VacancyService
import java.util.UUID

fun Route.vacancyRoutes(
    vacancyService: VacancyService
) {
    route("/api/vacancies") {

        get {
            val page = call.request.queryParameters["page"]
                ?.toIntOrNull()
                ?: 1

            val size = call.request.queryParameters["size"]
                ?.toIntOrNull()
                ?: 10

            val categoryId = call.request.queryParameters["categoryId"]

            val workFormat = call.request.queryParameters["workFormat"]
                ?.let { value ->
                    WorkFormatDto.valueOf(value)
                }

            val sort = call.request.queryParameters["sort"]
                ?: "createdAt_desc"

            // для поиска по названию вакансии
            val query = call.request.queryParameters["query"]

            val response = vacancyService.publicVacancies(
                VacancyFilterParams(
                    page = page,
                    size = size,
                    categoryId = categoryId,
                    workFormat = workFormat,
                    sort = sort,
                    query = query
                )
            )

            call.respond(response)
        }

        get("/{id}") {
            val id = UUID.fromString(
                call.parameters["id"]
            )

            val response = vacancyService.vacancyById(id)

            call.respond(response)
        }
    }
}