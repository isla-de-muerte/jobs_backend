package routes

import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import services.CategoryService

fun Route.categoryRoutes(
    categoryService: CategoryService
) {
    route("/api/categories") {
        get {
            call.respond(categoryService.getAll())
        }
    }
}