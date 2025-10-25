package com.codewithfk.domain.models

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant
import java.util.*

object RefreshTokens : UUIDTable("refresh_tokens") {
    val userId = reference("user_id", Users)
    val tokenHash = text("token_hash")
    val expiresAt = timestamp("expires_at")
    val revoked = bool("revoked")
    val createdAt = timestamp("created_at")
}

class RefreshToken(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<RefreshToken>(RefreshTokens)
    
    var userId by RefreshTokens.userId
    var tokenHash by RefreshTokens.tokenHash
    var expiresAt by RefreshTokens.expiresAt
    var revoked by RefreshTokens.revoked
    var createdAt by RefreshTokens.createdAt
}
