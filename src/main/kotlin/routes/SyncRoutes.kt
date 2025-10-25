package com.codewithfk.routes

import com.codewithfk.domain.dto.SyncRequest
import com.codewithfk.services.SyncService
import com.codewithfk.util.ProblemFactory
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.*

fun Route.syncRoutes() {
    val syncService = SyncService()
    
    authenticate("auth-jwt") {
        route("/sync") {
            post {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = UUID.fromString(principal?.payload?.getClaim("sub")?.asString() ?: "")
                    
                    val request = call.receive<SyncRequest>()
                    val response = syncService.sync(userId, request)
                    
                    call.respond(response)
                } catch (e: IllegalArgumentException) {
                    val problem = ProblemFactory.badRequest(e.message ?: "Invalid sync request")
                    call.respond(HttpStatusCode.BadRequest, problem)
                }
            }
        }
    }
}
