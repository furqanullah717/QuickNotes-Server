package com.codewithfk.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import com.codewithfk.util.ProblemFactory

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<IllegalArgumentException> { call, cause ->
            val problem = ProblemFactory.badRequest(cause.message ?: "Bad request")
            call.respond(HttpStatusCode.BadRequest, problem)
        }
        
        exception<SecurityException> { call, cause ->
            val problem = ProblemFactory.unauthorized(cause.message ?: "Unauthorized")
            call.respond(HttpStatusCode.Unauthorized, problem)
        }
        
        exception<NoSuchElementException> { call, cause ->
            val problem = ProblemFactory.notFound(cause.message ?: "Not found")
            call.respond(HttpStatusCode.NotFound, problem)
        }
        
        exception<Exception> { call, cause ->
            val problem = ProblemFactory.internalServerError("Internal server error")
            call.respond(HttpStatusCode.InternalServerError, problem)
        }
        
        status(HttpStatusCode.NotFound) { call, status ->
            val problem = ProblemFactory.notFound("The requested resource was not found")
            call.respond(status, problem)
        }
        
        status(HttpStatusCode.MethodNotAllowed) { call, status ->
            val problem = ProblemFactory.badRequest("Method not allowed")
            call.respond(status, problem)
        }
    }
}
