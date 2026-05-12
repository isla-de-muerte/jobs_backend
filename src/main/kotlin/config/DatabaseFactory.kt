package config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import db.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {

    fun init(config: DatabaseConfig) {
        val hikariConfig = HikariConfig().apply {
            jdbcUrl = config.url
            username = config.user
            password = config.password
            driverClassName = "org.postgresql.Driver"
            maximumPoolSize = 10
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        }

        Database.connect(HikariDataSource(hikariConfig))

        transaction {
            SchemaUtils.createMissingTablesAndColumns(
                Users,
                RefreshTokens,
                Categories,
                ApplicantProfiles,
                EmployerProfiles,
                Vacancies,
                Applications,
                ApplicationMessages,
                ApplicationChatReads,
                FavoriteVacancies
            )

            seedCategories()
        }
    }

    private fun seedCategories() {
        if (Categories.selectAll().count() > 0) return

        listOf(
            "IT и разработка" to "it-development",
            "Продажи" to "sales",
            "Маркетинг и PR" to "marketing-pr",
            "Менеджмент" to "management",
            "Финансы и бухгалтерия" to "finance-accounting",
            "HR и рекрутинг" to "hr-recruiting",
            "Дизайн" to "design",
            "Аналитика и данные" to "analytics-data",
            "Поддержка клиентов" to "customer-support",
            "Администрирование" to "administration",
            "Образование" to "education",
            "Медицина" to "medicine",
            "Юриспруденция" to "legal",
            "Логистика и склад" to "logistics-warehouse",
            "Производство" to "manufacturing",
            "Строительство" to "construction",
            "Розница" to "retail",
            "HoReCa" to "horeca",
            "Медиа и контент" to "media-content",
            "Без опыта / стажировки" to "internship-entry"
        ).forEach { (nameValue, slugValue) ->
            Categories.insert {
                it[name] = nameValue
                it[slug] = slugValue
            }
        }
    }
}