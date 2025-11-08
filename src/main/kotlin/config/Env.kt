package com.codewithfk.config

import java.io.File
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.*

data class DatabaseConfig(
    val jdbcUrl: String,
    val username: String,
    val password: String,
    val type: String
)

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
    
    private fun parseHerokuDatabaseUrl(url: String): DatabaseConfig? {
        return try {
            // Heroku format: postgres://user:password@host:port/database
            // or: postgresql://user:password@host:port/database
            // Note: Passwords with special characters are URL-encoded by Heroku
            val uri = URI(url)
            val scheme = uri.scheme
            val userInfo = uri.userInfo?.split(":", limit = 2) ?: return null
            val username = URLDecoder.decode(userInfo[0], StandardCharsets.UTF_8.name())
            val password = URLDecoder.decode(userInfo.getOrNull(1) ?: "", StandardCharsets.UTF_8.name())
            val host = uri.host
            val port = if (uri.port != -1) uri.port else when (scheme) {
                "postgres", "postgresql" -> 5432
                "mysql" -> 3306
                else -> 5432
            }
            val database = uri.path.removePrefix("/")
            
            val dbType = when (scheme) {
                "postgres", "postgresql" -> "postgresql"
                "mysql" -> "mysql"
                else -> "postgresql"
            }
            
            val jdbcUrl = when (dbType) {
                "mysql" -> "jdbc:mysql://$host:$port/$database?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
                else -> "jdbc:postgresql://$host:$port/$database"
            }
            
            DatabaseConfig(jdbcUrl, username, password, dbType)
        } catch (e: Exception) {
            null
        }
    }
    
    private val databaseConfig: DatabaseConfig by lazy {
        val herokuUrl = System.getenv("DATABASE_URL") ?: properties.getProperty("DATABASE_URL")
        
        if (herokuUrl != null && !herokuUrl.startsWith("jdbc:")) {
            // Try to parse as Heroku format
            parseHerokuDatabaseUrl(herokuUrl) ?: run {
                // Fall back to individual env vars
                val dbType = getEnv("DATABASE_TYPE", "postgresql")
                DatabaseConfig(
                    jdbcUrl = getEnv("DATABASE_URL", when (dbType.lowercase()) {
                        "mysql" -> "jdbc:mysql://localhost:3306/quickernotes?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
                        else -> "jdbc:postgresql://localhost:5432/quickernotes"
                    }),
                    username = getEnv("DB_USER", when (dbType.lowercase()) {
                        "mysql" -> "root"
                        else -> "postgres"
                    }),
                    password = getEnv("DB_PASSWORD", when (dbType.lowercase()) {
                        "mysql" -> "root1234"
                        else -> "root1234"
                    }),
                    type = dbType
                )
            }
        } else {
            // Already in JDBC format or using individual env vars
            val dbType = getEnv("DATABASE_TYPE", "postgresql")
            DatabaseConfig(
                jdbcUrl = getEnv("DATABASE_URL", when (dbType.lowercase()) {
                    "mysql" -> "jdbc:mysql://localhost:3306/quickernotes?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
                    else -> "jdbc:postgresql://localhost:5432/quickernotes"
                }),
                username = getEnv("DB_USER", when (dbType.lowercase()) {
                    "mysql" -> "root"
                    else -> "postgres"
                }),
                password = getEnv("DB_PASSWORD", when (dbType.lowercase()) {
                    "mysql" -> "root1234"
                    else -> "root1234"
                }),
                type = dbType
            )
        }
    }
    
    val DATABASE_TYPE = databaseConfig.type
    val DATABASE_URL = databaseConfig.jdbcUrl
    val DB_USER = databaseConfig.username
    val DB_PASSWORD = databaseConfig.password
    
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
