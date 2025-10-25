package com.codewithfk

import com.codewithfk.routes.authRoutes
import com.codewithfk.routes.syncRoutes
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {

        authRoutes()
        syncRoutes()

        get("/") {
            call.respondText("Hello World!")
        }

        get("/health") {
            call.respond(mapOf("status" to "ok"))
        }

        get("/version") {
            call.respond(mapOf("version" to "0.0.1"))
        }
    }
}
