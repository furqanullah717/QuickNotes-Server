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
    try {
        println("Starting QuickerNotes Server...")
        println("Port: ${Env.PORT}")
        println("Environment variables loaded")
        
        embeddedServer(Netty, port = Env.PORT, host = "0.0.0.0", module = Application::module)
            .start(wait = true)
    } catch (e: Exception) {
        println("FATAL ERROR: Application failed to start")
        println("Error: ${e.message}")
        e.printStackTrace()
        System.exit(1)
    }
}

fun Application.module() {
    try {
        // Initialize database
        println("Initializing database connection...")
        println("Database URL: ${Env.DATABASE_URL.take(50)}...") // Log partial URL for debugging
        println("Database Type: ${Env.DATABASE_TYPE}")
        println("Database User: ${Env.DB_USER}")
        
        DatabaseConfig.init(Env.DATABASE_URL, Env.DB_USER, Env.DB_PASSWORD, Env.DATABASE_TYPE)
        println("Database initialized successfully")
    } catch (e: Exception) {
        println("ERROR: Failed to initialize database: ${e.message}")
        e.printStackTrace()
        throw e // Re-throw to prevent app from starting with broken DB
    }
    
    try {
        // Configure plugins
        configureSerialization()
        configureMonitoring()
        configureCORS()
        configureStatusPages()
        configureSecurity()
        configureRouting()
        println("Plugins configured successfully")
    } catch (e: Exception) {
        println("ERROR: Failed to configure plugins: ${e.message}")
        e.printStackTrace()
        throw e
    }
    
    try {
        // Start scheduled task for processing account deletions
        startAccountDeletionScheduler()
        println("Account deletion scheduler started")
    } catch (e: Exception) {
        println("WARNING: Failed to start account deletion scheduler: ${e.message}")
        e.printStackTrace()
        // Don't throw - scheduler is not critical for app startup
    }
    
    println("Application started successfully on port ${Env.PORT}")
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