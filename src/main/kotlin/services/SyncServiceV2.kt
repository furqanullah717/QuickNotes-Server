package com.codewithfk.services

import com.codewithfk.domain.dto.NoteChangeV2
import com.codewithfk.domain.dto.SyncRequestV2
import com.codewithfk.domain.dto.SyncResponseV2
import com.codewithfk.domain.models.Note
import com.codewithfk.domain.models.Notes
import com.codewithfk.domain.models.Users
import com.codewithfk.util.Time
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import java.util.*

class SyncServiceV2 {
    
    fun sync(userId: UUID, request: SyncRequestV2): SyncResponseV2 {
        val now = Time.now()
        val since = request.since?.let { Time.parse(it) }
        
        return transaction {
            val applied = mutableListOf<String>()
            val conflicts = mutableListOf<NoteChangeV2>()
            
            // Process client changes
            for (clientChange in request.changes) {
                val noteId = UUID.fromString(clientChange.id)
                val clientUpdatedAt = Time.parse(clientChange.updatedAt) ?: continue
                
                val existingNote = Note.findById(noteId)
                
                if (existingNote == null) {
                    // Create new note
                    Note.new(noteId) {
                        this.userId = EntityID(userId, Users)
                        this.title = clientChange.title
                        this.body = clientChange.body
                        this.isDeleted = clientChange.isDeleted
                        this.isPinned = clientChange.isPinned
                        this.tags = clientChange.tags ?: ""
                        this.checklist = clientChange.checklist ?: ""
                        this.colorTag = clientChange.colorTag ?: ""
                        this.updatedAt = now
                    }
                    applied.add(clientChange.id)
                } else {
                    // Check if note belongs to user
                    if (existingNote.userId.value != userId) {
                        continue // Skip notes that don't belong to this user
                    }
                    
                    // Check for conflict (server version is newer)
                    if (existingNote.updatedAt.isAfter(clientUpdatedAt)) {
                        // Server version wins - add to conflicts
                        conflicts.add(
                            NoteChangeV2(
                                id = existingNote.id.value.toString(),
                                title = existingNote.title,
                                body = existingNote.body,
                                isDeleted = existingNote.isDeleted,
                                updatedAt = Time.format(existingNote.updatedAt),
                                isPinned = existingNote.isPinned,
                                tags = existingNote.tags ?: "",
                                checklist = existingNote.checklist ?: "",
                                colorTag = existingNote.colorTag ?: ""
                            )
                        )
                    } else {
                        // Client version is newer or equal - update note
                        existingNote.title = clientChange.title
                        existingNote.body = clientChange.body
                        existingNote.isDeleted = clientChange.isDeleted
                        existingNote.isPinned = clientChange.isPinned
                        existingNote.tags = clientChange.tags ?: ""
                        existingNote.checklist = clientChange.checklist ?: ""
                        existingNote.colorTag = clientChange.colorTag ?: ""
                        existingNote.updatedAt = now
                        applied.add(clientChange.id)
                    }
                }
            }
            
            // Get server changes since last sync
            val serverChanges = if (since != null) {
                Note.find { 
                    (Notes.userId eq userId) and 
                    (Notes.updatedAt greater since) and
                    (Notes.isDeleted eq false)
                }.orderBy(Notes.updatedAt to SortOrder.ASC)
                    .limit(1000)
                    .map { note ->
                        NoteChangeV2(
                            id = note.id.value.toString(),
                            title = note.title,
                            body = note.body,
                            isDeleted = note.isDeleted,
                            updatedAt = Time.format(note.updatedAt),
                            isPinned = note.isPinned,
                            tags = note.tags ?: "",
                            checklist = note.checklist ?: "",
                            colorTag = note.colorTag ?: ""
                        )
                    }
            } else {
                // First sync - return all non-deleted notes
                Note.find { 
                    (Notes.userId eq userId) and
                    (Notes.isDeleted eq false)
                }
                    .orderBy(Notes.updatedAt to SortOrder.ASC)
                    .limit(1000)
                    .map { note ->
                        NoteChangeV2(
                            id = note.id.value.toString(),
                            title = note.title,
                            body = note.body,
                            isDeleted = note.isDeleted,
                            updatedAt = Time.format(note.updatedAt),
                            isPinned = note.isPinned,
                            tags = note.tags ?: "",
                            checklist = note.checklist ?: "",
                            colorTag = note.colorTag ?: ""
                        )
                    }
            }
            
            SyncResponseV2(
                now = Time.format(now),
                applied = applied,
                conflicts = conflicts,
                changes = serverChanges,
                nextSince = Time.format(now)
            )
        }
    }
}

