package com.codewithfk.util

import io.ktor.http.*
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class Problem(
    val type: String,
    val title: String,
    val status: Int,
    val detail: String,
    val traceId: String = UUID.randomUUID().toString()
)

object ProblemFactory {
    fun badRequest(detail: String, type: String = "about:blank"): Problem {
        return Problem(
            type = type,
            title = "Bad Request",
            status = HttpStatusCode.BadRequest.value,
            detail = detail
        )
    }
    
    fun unauthorized(detail: String, type: String = "about:blank"): Problem {
        return Problem(
            type = type,
            title = "Unauthorized",
            status = HttpStatusCode.Unauthorized.value,
            detail = detail
        )
    }
    
    fun forbidden(detail: String, type: String = "about:blank"): Problem {
        return Problem(
            type = type,
            title = "Forbidden",
            status = HttpStatusCode.Forbidden.value,
            detail = detail
        )
    }
    
    fun notFound(detail: String, type: String = "about:blank"): Problem {
        return Problem(
            type = type,
            title = "Not Found",
            status = HttpStatusCode.NotFound.value,
            detail = detail
        )
    }
    
    fun conflict(detail: String, type: String = "about:blank"): Problem {
        return Problem(
            type = type,
            title = "Conflict",
            status = HttpStatusCode.Conflict.value,
            detail = detail
        )
    }
    
    fun tooManyRequests(detail: String, type: String = "about:blank"): Problem {
        return Problem(
            type = type,
            title = "Too Many Requests",
            status = HttpStatusCode.TooManyRequests.value,
            detail = detail
        )
    }
    
    fun internalServerError(detail: String, type: String = "about:blank"): Problem {
        return Problem(
            type = type,
            title = "Internal Server Error",
            status = HttpStatusCode.InternalServerError.value,
            detail = detail
        )
    }
}

