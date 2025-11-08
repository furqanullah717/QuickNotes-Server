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
    val isPinned = bool("is_pinned").default(false)
    // Note: TEXT columns can't have defaults in MySQL, so we make them nullable and handle nulls as empty strings in code
    val tags = text("tags").nullable()
    val checklist = text("checklist").nullable()
    val colorTag = text("color_tag").nullable()
}

class Note(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Note>(Notes)
    
    var userId by Notes.userId
    var title by Notes.title
    var body by Notes.body
    var isDeleted by Notes.isDeleted
    var updatedAt by Notes.updatedAt
    var isPinned by Notes.isPinned
    var tags by Notes.tags
    var checklist by Notes.checklist
    var colorTag by Notes.colorTag
}
