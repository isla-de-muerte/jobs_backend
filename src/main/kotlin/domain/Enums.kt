package domain

enum class UserRole {
    APPLICANT,
    EMPLOYER
}

enum class WorkFormat {
    REMOTE,
    PART_TIME,
    HYBRID,
    OFFICE
}

enum class VacancyStatus {
    DRAFT,
    PUBLISHED,
    PAUSED_BY_OVERLOAD,
    ARCHIVED
}

enum class ApplicationStatus {
    NEW,
    VIEWED,
    INTERVIEW,
    REJECTED,
    ACCEPTED
}

enum class SortDirection {
    ASC,
    DESC
}

enum class VacancySort {
    CREATED_AT,
    PUBLISHED_AT
}