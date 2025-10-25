package com.codewithfk.data

import com.codewithfk.domain.models.*
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseConfig {
    private var dataSource: HikariDataSource? = null
    
    fun init(databaseUrl: String, username: String, password: String, databaseType: String = "postgresql") {
        val config = HikariConfig().apply {
            jdbcUrl = databaseUrl
            this.username = username
            this.password = password
            
            driverClassName = when (databaseType.lowercase()) {
                "mysql" -> "com.mysql.cj.jdbc.Driver"
                else -> "org.postgresql.Driver"
            }
            
            maximumPoolSize = 10
            minimumIdle = 2
            connectionTimeout = 30000
            idleTimeout = 600000
            maxLifetime = 1800000
            
            if (databaseType.lowercase() == "mysql") {
                addDataSourceProperty("cachePrepStmts", "true")
                addDataSourceProperty("prepStmtCacheSize", "250")
                addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
                addDataSourceProperty("useServerPrepStmts", "true")
                addDataSourceProperty("useLocalSessionState", "true")
                addDataSourceProperty("rewriteBatchedStatements", "true")
                addDataSourceProperty("cacheResultSetMetadata", "true")
                addDataSourceProperty("cacheServerConfiguration", "true")
                addDataSourceProperty("elideSetAutoCommits", "true")
                addDataSourceProperty("maintainTimeStats", "false")
                addDataSourceProperty("allowPublicKeyRetrieval", "true")
                addDataSourceProperty("useSSL", "false")
                addDataSourceProperty("serverTimezone", "UTC")
            }
        }
        
        dataSource = HikariDataSource(config)
        Database.connect(dataSource!!)
        
        transaction {
            SchemaUtils.createMissingTablesAndColumns(
                Users,
                Notes,
                RefreshTokens,
                PasswordResets
            )
        }
    }
    
    fun close() {
        dataSource?.close()
    }
}
