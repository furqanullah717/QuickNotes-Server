package com.codewithfk.domain.dto

import com.codewithfk.domain.models.ReminderRepeatType
import kotlinx.serialization.Serializable

@Serializable
data class SyncRequestV2(
    val since: String? = null,
    val changes: List<NoteChangeV2>,
    val reminderChanges: List<ReminderChangeV2> = emptyList()
)

@Serializable
data class NoteChangeV2(
    val id: String,
    val title: String,
    val body: String,
    val isDeleted: Boolean,
    val updatedAt: String,
    val isPinned: Boolean = false,
    val tags: String = "",
    val checklist: String = "",
    val colorTag: String = ""
)

@Serializable
data class ReminderChangeV2(
    val id: String,
    val noteId: String,
    val title: String,
    val body: String,
    val scheduledAtEpochMillis: Long,
    val repeatType: ReminderRepeatType,
    val isEnabled: Boolean,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val isDeleted: Boolean = false
)

@Serializable
data class SyncResponseV2(
    val now: String,
    val applied: List<String>,
    val conflicts: List<NoteChangeV2>,
    val changes: List<NoteChangeV2>,
    val nextSince: String,
    val appliedReminders: List<String> = emptyList(),
    val conflictsReminders: List<ReminderChangeV2> = emptyList(),
    val reminderChanges: List<ReminderChangeV2> = emptyList()
)

