package com.codewithfk.domain.models

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant
import java.util.*

object PasswordResets : UUIDTable("password_resets") {
    val userId = reference("user_id", Users)
    val codeHash = text("code_hash")
    val expiresAt = timestamp("expires_at")
    val used = bool("used")
    val createdAt = timestamp("created_at")
}

class PasswordReset(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<PasswordReset>(PasswordResets)
    
    var userId by PasswordResets.userId
    var codeHash by PasswordResets.codeHash
    var expiresAt by PasswordResets.expiresAt
    var used by PasswordResets.used
    var createdAt by PasswordResets.createdAt
}
