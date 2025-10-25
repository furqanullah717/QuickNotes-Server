package com.codewithfk.domain.models

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant
import java.util.*

object Notes : UUIDTable("notes") {
    val userId = reference("user_id", Users)
    val title = text("title")
    val body = text("body")
    val isDeleted = bool("is_deleted")
    val updatedAt = timestamp("updated_at")
}

class Note(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Note>(Notes)
    
    var userId by Notes.userId
    var title by Notes.title
    var body by Notes.body
    var isDeleted by Notes.isDeleted
    var updatedAt by Notes.updatedAt
}
