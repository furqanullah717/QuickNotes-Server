package com.codewithfk

import com.codewithfk.config.Env
import com.codewithfk.data.DatabaseConfig
import com.codewithfk.plugins.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

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
}