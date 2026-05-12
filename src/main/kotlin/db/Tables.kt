package db

import domain.ApplicationStatus
import domain.UserRole
import domain.VacancyStatus
import domain.WorkFormat
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object Users : UUIDTable("users") {
    val email = varchar("email", 255).uniqueIndex()
    val passwordHash = text("password_hash")
    val role = enumerationByName<UserRole>("role", 32)
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
    val updatedAt = timestamp("updated_at").clientDefault { Instant.now() }
}

object RefreshTokens : UUIDTable("refresh_tokens") {
    val userId = reference("user_id", Users, onDelete = ReferenceOption.CASCADE)
    val tokenHash = text("token_hash").uniqueIndex()
    val revoked = bool("revoked").default(false)
    val expiresAt = timestamp("expires_at")
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
}

object Categories : UUIDTable("categories") {
    val name = varchar("name", 120).uniqueIndex()
    val slug = varchar("slug", 120).uniqueIndex()
}

object ApplicantProfiles : UUIDTable("applicant_profiles") {
    val userId = reference("user_id", Users, onDelete = ReferenceOption.CASCADE).uniqueIndex()
    val fullName = varchar("full_name", 255)
    val contacts = text("contacts")
    val skills = text("skills")
    val experience = text("experience").nullable()
    val education = text("education").nullable()
    val portfolioUrl = text("portfolio_url").nullable()
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
    val updatedAt = timestamp("updated_at").clientDefault { Instant.now() }
}

object EmployerProfiles : UUIDTable("employer_profiles") {
    val userId = reference("user_id", Users, onDelete = ReferenceOption.CASCADE).uniqueIndex()
    val companyName = varchar("company_name", 255)
    val description = text("description").nullable()
    val website = text("website").nullable()
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
    val updatedAt = timestamp("updated_at").clientDefault { Instant.now() }
}

object Vacancies : UUIDTable("vacancies") {
    val employerId = reference("employer_id", Users, onDelete = ReferenceOption.CASCADE)
    val categoryId = reference("category_id", Categories)
    val title = varchar("title", 255)
    val description = text("description")
    val requirements = text("requirements").nullable()
    val salaryFrom = integer("salary_from").nullable()
    val salaryTo = integer("salary_to").nullable()
    val workFormat = enumerationByName<WorkFormat>("work_format", 32)
    val status = enumerationByName<VacancyStatus>("status", 32).default(VacancyStatus.DRAFT)
    val viewsCount = integer("views_count").default(0)
    val applicationsCount = integer("applications_count").default(0)
    val publishedAt = timestamp("published_at").nullable()
    val pausedAt = timestamp("paused_at").nullable()
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
    val updatedAt = timestamp("updated_at").clientDefault { Instant.now() }

    init {
        index(false, status, createdAt)
        index(false, categoryId)
        index(false, workFormat)
        index(false, employerId)
    }
}

object Applications : UUIDTable("applications") {
    val vacancyId = reference("vacancy_id", Vacancies, onDelete = ReferenceOption.CASCADE)
    val applicantId = reference("applicant_id", Users, onDelete = ReferenceOption.CASCADE)
    val profileId = reference("profile_id", ApplicantProfiles)
    val coverLetter = text("cover_letter").nullable()
    val status = enumerationByName<ApplicationStatus>("status", 32).default(ApplicationStatus.NEW)
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
    val updatedAt = timestamp("updated_at").clientDefault { Instant.now() }

    init {
        uniqueIndex(vacancyId, applicantId)
        index(false, vacancyId)
        index(false, applicantId)
    }
}

object ApplicationChatReads : UUIDTable("application_chat_reads") {
    val applicationId = reference("application_id", Applications, onDelete = ReferenceOption.CASCADE)
    val userId = reference("user_id", Users, onDelete = ReferenceOption.CASCADE)
    val lastReadAt = timestamp("last_read_at")
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
    val updatedAt = timestamp("updated_at").clientDefault { Instant.now() }

    init {
        uniqueIndex(applicationId, userId)
        index(false, applicationId)
        index(false, userId)
    }
}

object ApplicationMessages : UUIDTable("application_messages") {
    val applicationId = reference("application_id", Applications, onDelete = ReferenceOption.CASCADE)
    val senderId = reference("sender_id", Users, onDelete = ReferenceOption.CASCADE)
    val senderRole = enumerationByName<UserRole>("sender_role", 32)
    val message = text("message")
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }

    init {
        index(false, applicationId)
        index(false, senderId)
        index(false, createdAt)
    }
}

object FavoriteVacancies : UUIDTable("favorite_vacancies") {
    val applicantId = reference("applicant_id", Users, onDelete = ReferenceOption.CASCADE)
    val vacancyId = reference("vacancy_id", Vacancies, onDelete = ReferenceOption.CASCADE)
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }

    init {
        uniqueIndex(applicantId, vacancyId)
        index(false, applicantId)
        index(false, vacancyId)
    }
}
