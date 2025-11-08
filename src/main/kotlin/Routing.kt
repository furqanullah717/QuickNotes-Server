package com.codewithfk

import com.codewithfk.routes.accountDeletionRoutes
import com.codewithfk.routes.authRoutes
import com.codewithfk.routes.syncRoutes
import com.codewithfk.routes.syncRoutesV2
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {

        authRoutes()
        syncRoutes() // V1 API - backward compatibility
        syncRoutesV2() // V2 API - new fields support
        accountDeletionRoutes()

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
