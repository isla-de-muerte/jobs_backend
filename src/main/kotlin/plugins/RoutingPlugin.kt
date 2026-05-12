package plugins

import config.AppConfig
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import repositories.ApplicationRepository
import repositories.CategoryRepository
import repositories.FavoriteRepository
import repositories.ProfileRepository
import repositories.RefreshTokenRepository
import repositories.UserRepository
import repositories.VacancyRepository
import routes.applicationRoutes
import routes.authRoutes
import routes.categoryRoutes
import routes.employerVacancyRoutes
import routes.favoriteRoutes
import routes.profileRoutes
import routes.vacancyRoutes
import security.JwtService
import security.PasswordHasher
import services.ApplicationService
import services.AuthService
import services.CategoryService
import services.FavoriteService
import services.HiringResponsibilityService
import services.ProfileService
import services.VacancyService
import repositories.ApplicationMessageRepository
import routes.applicationMessageRoutes
import services.ApplicationMessageService

fun Application.configureRouting(
    appConfig: AppConfig
) {
    val userRepository = UserRepository()
    val refreshTokenRepository = RefreshTokenRepository()
    val profileRepository = ProfileRepository()
    val vacancyRepository = VacancyRepository()
    val applicationRepository = ApplicationRepository()
    val favoriteRepository = FavoriteRepository()
    val categoryRepository = CategoryRepository()
    val applicationMessageRepository = ApplicationMessageRepository()
    val passwordHasher = PasswordHasher()
    val jwtService = JwtService(appConfig.jwt)

    val hiringResponsibilityService = HiringResponsibilityService()

    val authService = AuthService(
        userRepository = userRepository,
        refreshTokenRepository = refreshTokenRepository,
        passwordHasher = passwordHasher,
        jwtService = jwtService,
        jwtConfig = appConfig.jwt
    )
    val applicationMessageService = ApplicationMessageService(
        repository = applicationMessageRepository
    )

    val profileService = ProfileService(
        profileRepository = profileRepository
    )

    val vacancyService = VacancyService(
        repository = vacancyRepository,
        hiringResponsibilityService = hiringResponsibilityService
    )

    val applicationService = ApplicationService(
        repository = applicationRepository,
        messageRepository = applicationMessageRepository,
        hiringResponsibilityService = hiringResponsibilityService
    )

    val favoriteService = FavoriteService(
        repository = favoriteRepository
    )

    val categoryService = CategoryService(
        repository = categoryRepository
    )

    routing {
        get("/health") {
            call.respond(mapOf("status" to "ok"))
        }

        authRoutes(authService)
        profileRoutes(profileService)
        employerVacancyRoutes(vacancyService)
        vacancyRoutes(vacancyService)
        applicationRoutes(applicationService)
        favoriteRoutes(favoriteService)
        categoryRoutes(categoryService)
        applicationMessageRoutes(applicationMessageService)
    }
}