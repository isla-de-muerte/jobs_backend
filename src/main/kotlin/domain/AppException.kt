package domain

import io.ktor.http.HttpStatusCode

class AppException(
    val status: HttpStatusCode,
    val code: String,
    override val message: String
) : RuntimeException(message)