package plugins

import dto.ErrorResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.request.path
import io.ktor.server.response.respond
import java.util.concurrent.ConcurrentHashMap

private data class RateLimitBucket(
    val timestamps: MutableList<Long> = mutableListOf()
)

class SimpleRateLimiter(
    private val limit: Int,
    private val windowMillis: Long
) {
    private val buckets = ConcurrentHashMap<String, RateLimitBucket>()

    fun isAllowed(key: String): Boolean {
        val now = System.currentTimeMillis()
        val windowStart = now - windowMillis

        val bucket = buckets.computeIfAbsent(key) {
            RateLimitBucket()
        }

        synchronized(bucket) {
            bucket.timestamps.removeIf { it < windowStart }

            if (bucket.timestamps.size >= limit) {
                return false
            }

            bucket.timestamps.add(now)
            return true
        }
    }
}

fun Application.configureRateLimit() {
    val limiter = SimpleRateLimiter(
        limit = 60,
        windowMillis = 60_000
    )

    intercept(ApplicationCallPipeline.Call) {
        val path = call.request.path()

        val shouldLimit =
            path.startsWith("/api/auth") ||
                    path.startsWith("/api/vacancies")

        if (!shouldLimit) {
            proceed()
            return@intercept
        }

        val ip = call.request.local.remoteHost
        val key = "$ip:$path"

        if (!limiter.isAllowed(key)) {
            call.respond(
                HttpStatusCode.TooManyRequests,
                ErrorResponse(
                    status = 429,
                    code = "RATE_LIMIT_EXCEEDED",
                    message = "Too many requests. Try again later."
                )
            )
            finish()
        }
    }
}