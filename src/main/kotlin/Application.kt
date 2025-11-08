package com.codewithfk

import com.codewithfk.config.Env
import com.codewithfk.data.DatabaseConfig
import com.codewithfk.plugins.*
import com.codewithfk.services.AccountDeletionService
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.*

fun main() {
    embeddedServer(Netty, port = Env.PORT, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    // Initialize database
    DatabaseConfig.init(Env.DATABASE_URL, Env.DB_USER, Env.DB_PASSWORD, Env.DATABASE_TYPE)
    
    // Configure plugins
    configureSerialization()
    configureMonitoring()
    configureCORS()
    configureStatusPages()
    configureSecurity()
    configureRouting()
    
    // Start scheduled task for processing account deletions
    startAccountDeletionScheduler()
}

private fun Application.startAccountDeletionScheduler() {
    val accountDeletionService = AccountDeletionService()
    var lastCleanup = System.currentTimeMillis()
    
    // Run every hour to check for scheduled deletions
    CoroutineScope(Dispatchers.Default).launch {
        while (isActive) {
            try {
                accountDeletionService.processScheduledDeletions()
                
                // Cleanup old deletion records (run once per day)
                val now = System.currentTimeMillis()
                if (now - lastCleanup >= 24 * 60 * 60 * 1000) {
                    accountDeletionService.cleanupOldDeletionRecords()
                    lastCleanup = now
                }
            } catch (e: Exception) {
                println("Error in account deletion scheduler: ${e.message}")
                e.printStackTrace()
            }
            delay(3600000) // Wait 1 hour
        }
    }
}