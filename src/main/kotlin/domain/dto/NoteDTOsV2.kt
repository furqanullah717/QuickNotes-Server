package com.codewithfk.domain.dto

import kotlinx.serialization.Serializable

@Serializable
data class SyncRequestV2(
    val since: String? = null,
    val changes: List<NoteChangeV2>
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
data class SyncResponseV2(
    val now: String,
    val applied: List<String>,
    val conflicts: List<NoteChangeV2>,
    val changes: List<NoteChangeV2>,
    val nextSince: String
)

