package com.codewithfk.config

import java.io.File
import java.util.*

object Env {
    private val properties = loadEnvFile()
    
    private fun loadEnvFile(): Properties {
        val props = Properties()
        val envFile = File(".env")
        
        if (envFile.exists()) {
            try {
                envFile.inputStream().use { input ->
                    props.load(input)
                }
            } catch (e: Exception) {
                println("Warning: Could not load .env file: ${e.message}")
            }
        }
        
        return props
    }
    
    private fun getEnv(key: String, defaultValue: String): String {
        return System.getenv(key) ?: properties.getProperty(key) ?: defaultValue
    }
    
    private fun getEnvInt(key: String, defaultValue: Int): Int {
        val value = System.getenv(key) ?: properties.getProperty(key)
        return value?.toIntOrNull() ?: defaultValue
    }
    
    val DATABASE_TYPE = getEnv("DATABASE_TYPE", "mysql") // postgresql or mysql
    val DATABASE_URL: String = getEnv("DATABASE_URL", when (DATABASE_TYPE.lowercase()) {
        "mysql" -> "jdbc:mysql://localhost:3306/quickernotes?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
        else -> "jdbc:postgresql://localhost:5432/quickernotes"
    })
    val DB_USER = getEnv("DB_USER", when (DATABASE_TYPE.lowercase()) {
        "mysql" -> "root"
        else -> "postgres"
    })
    val DB_PASSWORD = getEnv("DB_PASSWORD", when (DATABASE_TYPE.lowercase()) {
        "mysql" -> "root1234"
        else -> "root1234"
    })
    
    val JWT_SECRET = getEnv("JWT_SECRET", "default-secret-change-in-production")
    val ACCESS_TOKEN_TTL_MIN = getEnvInt("ACCESS_TOKEN_TTL_MIN", 2592000)
    val REFRESH_TOKEN_TTL_DAYS = getEnvInt("REFRESH_TOKEN_TTL_DAYS", 30)
    
    val SMTP_HOST = getEnv("SMTP_HOST", "localhost")
    val SMTP_PORT = getEnvInt("SMTP_PORT", 1025)
    val SMTP_USER = getEnv("SMTP_USER", "")
    val SMTP_PASS = getEnv("SMTP_PASS", "")
    val EMAIL_FROM = getEnv("EMAIL_FROM", "noreply@quickernotes.local")
    
    val APP_BASE_URL = getEnv("APP_BASE_URL", "http://localhost:8080")
    val PORT = getEnvInt("PORT", 8080)
}
