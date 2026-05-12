import config.AppConfig
import config.DatabaseFactory
import io.ktor.server.application.Application
import plugins.*

fun Application.module() {
    val appConfig = AppConfig(environment.config)

    DatabaseFactory.init(appConfig.database)

    configureSerialization()
    configureStatusPages()
    configureCors()
    configureAuth(appConfig.jwt)
    configureRouting(appConfig)
    configureRateLimit()
}