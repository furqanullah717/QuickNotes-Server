package com.codewithfk.domain.models

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant
import java.util.*

@Serializable
enum class ReminderRepeatType {
    NONE,
    DAILY,
    WEEKLY
}

object Reminders : UUIDTable("reminders") {
    val userId = reference("user_id", Users)
    val noteId = reference("note_id", Notes)
    val title = text("title")
    val body = text("body")
    val scheduledAtEpochMillis = long("scheduled_at_epoch_millis")
    val repeatType = enumerationByName<ReminderRepeatType>("repeat_type", 20)
    val isEnabled = bool("is_enabled")
    val isDeleted = bool("is_deleted").default(false)
    val createdAtEpochMillis = long("created_at_epoch_millis")
    val updatedAtEpochMillis = long("updated_at_epoch_millis")
    val updatedAt = timestamp("updated_at")
}

class Reminder(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Reminder>(Reminders)
    
    var userId by Reminders.userId
    var noteId by Reminders.noteId
    var title by Reminders.title
    var body by Reminders.body
    var scheduledAtEpochMillis by Reminders.scheduledAtEpochMillis
    var repeatType by Reminders.repeatType
    var isEnabled by Reminders.isEnabled
    var isDeleted by Reminders.isDeleted
    var createdAtEpochMillis by Reminders.createdAtEpochMillis
    var updatedAtEpochMillis by Reminders.updatedAtEpochMillis
    var updatedAt by Reminders.updatedAt
}

