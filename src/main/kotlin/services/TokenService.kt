package com.codewithfk.services

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.codewithfk.config.Security
import com.codewithfk.domain.models.RefreshToken
import com.codewithfk.domain.models.RefreshTokens
import com.codewithfk.domain.models.User
import com.codewithfk.domain.models.Users
import com.codewithfk.util.Hashing
import com.codewithfk.util.Time
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import java.util.*

class TokenService(
    private val jwtSecret: String,
    private val accessTokenTtlMinutes: Int,
    private val refreshTokenTtlDays: Int
) {
    
    fun generateAccessToken(userId: UUID): String {
        val algorithm = Algorithm.HMAC256(jwtSecret)
        val now = Time.now()
        
        return JWT.create()
            .withSubject(userId.toString())
            .withIssuer(Security.ISSUER)
            .withAudience(Security.AUDIENCE)
            .withIssuedAt(Date.from(now))
            .withExpiresAt(Date.from(now.plusSeconds(accessTokenTtlMinutes * 60L)))
            .sign(algorithm)
    }
    
    fun generateRefreshToken(userId: UUID): String {
        val token = Hashing.generateRandomToken()
        val tokenHash = Hashing.hashToken(token)
        val expiresAt = Time.now().plusSeconds(refreshTokenTtlDays * 24 * 60 * 60L)
        
        transaction {
            RefreshToken.new {
                this.userId = EntityID(userId, Users)
                this.tokenHash = tokenHash
                this.expiresAt = expiresAt
                this.revoked = false
                this.createdAt = Time.now()
            }
        }
        
        return token
    }
    
    fun validateAccessToken(token: String): UUID? {
        return try {
            val algorithm = Algorithm.HMAC256(jwtSecret)
            val verifier = JWT.require(algorithm).build()
            val decodedJWT = verifier.verify(token)
            UUID.fromString(decodedJWT.subject)
        } catch (e: Exception) {
            null
        }
    }
    
    fun validateRefreshToken(token: String): UUID? {
        val tokenHash = Hashing.hashToken(token)
        
        return transaction {
            val refreshToken = RefreshToken.find { 
                RefreshTokens.tokenHash eq tokenHash 
            }.firstOrNull()
            
            if (refreshToken == null || refreshToken.revoked || refreshToken.expiresAt.isBefore(Time.now())) {
                null
            } else {
                refreshToken.userId.value
            }
        }
    }
    
    fun revokeRefreshToken(token: String) {
        val tokenHash = Hashing.hashToken(token)
        
        transaction {
            RefreshToken.find { RefreshTokens.tokenHash eq tokenHash }
                .forEach { it.revoked = true }
        }
    }
    
    fun revokeAllUserTokens(userId: UUID) {
        transaction {
            RefreshToken.find { RefreshTokens.userId eq userId }
                .forEach { it.revoked = true }
        }
    }
    
    fun cleanupExpiredTokens() {
        transaction {
            RefreshTokens.deleteWhere { RefreshTokens.expiresAt less Time.now() }
        }
    }
}
