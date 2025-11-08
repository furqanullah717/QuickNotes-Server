package com.codewithfk.routes

import com.codewithfk.domain.dto.SyncRequestV2
import com.codewithfk.services.SyncServiceV2
import com.codewithfk.util.ProblemFactory
import com.codewithfk.util.Time
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.*
import java.util.*

fun Route.syncRoutesV2() {
    val syncService = SyncServiceV2()
    
    authenticate("auth-jwt") {
        route("/api/v2/sync") {
            post {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = UUID.fromString(principal?.payload?.getClaim("sub")?.asString() ?: "")
                    
                    println("Sync V2 Request received for user $userId")
                    
                    val request = call.receive<SyncRequestV2>()
                    println("Request parsed successfully. Changes count: ${request.changes.size}, since: ${request.since}")
                    
                    // Validate request
                    validateSyncRequestV2(request)
                    println("Request validation passed")
                    
                    val response = syncService.sync(userId, request)
                    
                    call.respond(response)
                } catch (e: kotlinx.serialization.SerializationException) {
                    println("Serialization error in sync V2: ${e.message}")
                    println("Error cause: ${e.cause?.message}")
                    e.printStackTrace()
                    val problem = ProblemFactory.badRequest("Invalid request format: ${e.message ?: "Unable to parse request body"}")
                    call.respond(HttpStatusCode.BadRequest, problem)
                } catch (e: IllegalArgumentException) {
                    println("Validation error in sync V2: ${e.message}")
                    val problem = ProblemFactory.badRequest(e.message ?: "Invalid sync request")
                    call.respond(HttpStatusCode.BadRequest, problem)
                } catch (e: Exception) {
                    println("Unexpected error in sync V2: ${e.message}")
                    println("Error type: ${e.javaClass.name}")
                    e.printStackTrace()
                    val problem = ProblemFactory.badRequest("Invalid sync request: ${e.message ?: "Unknown error"}")
                    call.respond(HttpStatusCode.BadRequest, problem)
                }
            }
        }
    }
}

private fun validateSyncRequestV2(request: SyncRequestV2) {
    for ((changeIndex, change) in request.changes.withIndex()) {
        println("Validating change $changeIndex: id=${change.id}, title=${change.title.take(50)}")
        println("  - checklist: '${change.checklist.take(100)}' (length: ${change.checklist.length})")
        println("  - tags: '${change.tags}'")
        println("  - colorTag: '${change.colorTag}'")
        // Validate UUID format
        try {
            UUID.fromString(change.id)
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid note ID format: ${change.id}")
        }
        
        // Validate title
        if (change.title.length > 500) {
            throw IllegalArgumentException("Title exceeds maximum length of 500 characters")
        }
        
        // Validate body
        if (change.body.length > 10000) {
            throw IllegalArgumentException("Body exceeds maximum length of 10000 characters")
        }
        
        // Validate tags
        if (change.tags.isNotEmpty()) {
            val tagList = change.tags.split(",").map { it.trim() }
            if (tagList.size > 50) {
                throw IllegalArgumentException("Maximum 50 tags allowed")
            }
            for (tag in tagList) {
                if (tag.length > 30) {
                    throw IllegalArgumentException("Tag exceeds maximum length of 30 characters: $tag")
                }
            }
        }
        
        // Validate checklist JSON - only validate if not empty
        val trimmedChecklist = change.checklist.trim()
        if (trimmedChecklist.isNotEmpty()) {
            try {
                // Basic JSON validation - check if it's valid JSON array format
                if (!trimmedChecklist.startsWith("[") || !trimmedChecklist.endsWith("]")) {
                    throw IllegalArgumentException("Checklist must be a JSON array")
                }
                // Parse and validate structure
                val json = kotlinx.serialization.json.Json
                val checklist = json.parseToJsonElement(trimmedChecklist)
                if (checklist is kotlinx.serialization.json.JsonArray) {
                    for ((index, item) in checklist.withIndex()) {
                        if (item is kotlinx.serialization.json.JsonObject) {
                            // Log the actual structure for debugging
                            println("Checklist item $index keys: ${item.keys.joinToString()}")
                            
                            // Check for required fields
                            val hasId = item.containsKey("id")
                            val hasText = item.containsKey("text")
                            // isChecked/checked is optional - defaults to false if missing
                            
                            if (!hasId || !hasText) {
                                val missing = mutableListOf<String>()
                                if (!hasId) missing.add("id")
                                if (!hasText) missing.add("text")
                                throw IllegalArgumentException("Invalid checklist item at index $index: missing required fields ${missing.joinToString()}. Found keys: ${item.keys.joinToString()}")
                            }
                            
                            val text = item["text"]?.jsonPrimitive?.content ?: ""
                            if (text.length > 200) {
                                throw IllegalArgumentException("Checklist item text at index $index exceeds maximum length of 200 characters")
                            }
                        } else {
                            throw IllegalArgumentException("Checklist item at index $index must be a JSON object, found: ${item::class.simpleName}")
                        }
                    }
                } else {
                    throw IllegalArgumentException("Checklist must be a JSON array")
                }
            } catch (e: IllegalArgumentException) {
                // Re-throw validation errors
                throw e
            } catch (e: Exception) {
                throw IllegalArgumentException("Invalid checklist format: ${e.message}")
            }
        }
        
        // Validate colorTag (hex color format) - only validate if not empty
        if (change.colorTag.isNotEmpty()) {
            if (!change.colorTag.matches(Regex("^#[0-9A-Fa-f]{6}$"))) {
                throw IllegalArgumentException("Invalid colorTag format. Must be hex color (e.g., #FF6B6B)")
            }
        }
        
        // Validate updatedAt timestamp
        try {
            Time.parse(change.updatedAt)
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid updatedAt timestamp format: ${change.updatedAt}")
        }
    }
}

