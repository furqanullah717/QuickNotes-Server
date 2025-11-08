package com.codewithfk.domain.models

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant
import java.util.*

object AccountDeletions : UUIDTable("account_deletions") {
    val userId = reference("user_id", Users).uniqueIndex()
    val requestedAt = timestamp("requested_at")
    val scheduledDeletionAt = timestamp("scheduled_deletion_at")
    val deletedAt = timestamp("deleted_at").nullable()
    val createdAt = timestamp("created_at")
}

class AccountDeletion(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<AccountDeletion>(AccountDeletions)
    
    var userId by AccountDeletions.userId
    var requestedAt by AccountDeletions.requestedAt
    var scheduledDeletionAt by AccountDeletions.scheduledDeletionAt
    var deletedAt by AccountDeletions.deletedAt
    var createdAt by AccountDeletions.createdAt
}

