package config

import io.ktor.server.config.ApplicationConfig

data class AppConfig(
    val database: DatabaseConfig,
    val jwt: JwtConfig
) {
    constructor(config: ApplicationConfig) : this(
        database = DatabaseConfig(
            url = config.property("database.url").getString(),
            user = config.property("database.user").getString(),
            password = config.property("database.password").getString()
        ),
        jwt = JwtConfig(
            issuer = config.property("jwt.issuer").getString(),
            audience = config.property("jwt.audience").getString(),
            realm = config.property("jwt.realm").getString(),
            accessSecret = config.property("jwt.accessSecret").getString(),
            refreshSecret = config.property("jwt.refreshSecret").getString(),
            accessTtlMinutes = config.property("jwt.accessTtlMinutes").getString().toLong(),
            refreshTtlDays = config.property("jwt.refreshTtlDays").getString().toLong()
        )
    )
}

data class DatabaseConfig(
    val url: String,
    val user: String,
    val password: String
)

data class JwtConfig(
    val issuer: String,
    val audience: String,
    val realm: String,
    val accessSecret: String,
    val refreshSecret: String,
    val accessTtlMinutes: Long,
    val refreshTtlDays: Long
)