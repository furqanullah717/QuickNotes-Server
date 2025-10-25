package com.codewithfk.services

import com.codewithfk.domain.dto.NoteChange
import com.codewithfk.domain.dto.SyncRequest
import com.codewithfk.domain.dto.SyncResponse
import com.codewithfk.domain.models.Note
import com.codewithfk.domain.models.Notes
import com.codewithfk.domain.models.Users
import com.codewithfk.util.Time
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import java.util.*

class SyncService {
    
    fun sync(userId: UUID, request: SyncRequest): SyncResponse {
        val now = Time.now()
        val since = request.since?.let { Time.parse(it) }
        
        return transaction {
            val applied = mutableListOf<String>()
            val conflicts = mutableListOf<NoteChange>()
            
            for (clientChange in request.changes) {
                val noteId = UUID.fromString(clientChange.id)
                val clientUpdatedAt = Time.parse(clientChange.updatedAt) ?: continue
                
                val existingNote = Note.findById(noteId)
                
                if (existingNote == null) {
                    Note.new(noteId) {
                        this.userId = EntityID(userId, Users)
                        this.title = clientChange.title
                        this.body = clientChange.body
                        this.isDeleted = clientChange.isDeleted
                        this.updatedAt = now
                    }
                    applied.add(clientChange.id)
                } else {
                    if (clientUpdatedAt.isAfter(existingNote.updatedAt)) {
                        existingNote.title = clientChange.title
                        existingNote.body = clientChange.body
                        existingNote.isDeleted = clientChange.isDeleted
                        existingNote.updatedAt = now
                        applied.add(clientChange.id)
                    } else {
                        conflicts.add(
                            NoteChange(
                                id = existingNote.id.value.toString(),
                                title = existingNote.title,
                                body = existingNote.body,
                                isDeleted = existingNote.isDeleted,
                                updatedAt = Time.format(existingNote.updatedAt)
                            )
                        )
                    }
                }
            }
            
            val serverChanges = if (since != null) {
                Note.find { 
                    (Notes.userId eq userId) and 
                    (Notes.updatedAt greater since)
                }.orderBy(Notes.updatedAt to SortOrder.ASC)
                    .limit(1000)
                    .map { note ->
                        NoteChange(
                            id = note.id.value.toString(),
                            title = note.title,
                            body = note.body,
                            isDeleted = note.isDeleted,
                            updatedAt = Time.format(note.updatedAt)
                        )
                    }
            } else {
                Note.find { Notes.userId eq userId }
                    .orderBy(Notes.updatedAt to SortOrder.ASC)
                    .limit(1000)
                    .map { note ->
                        NoteChange(
                            id = note.id.value.toString(),
                            title = note.title,
                            body = note.body,
                            isDeleted = note.isDeleted,
                            updatedAt = Time.format(note.updatedAt)
                        )
                    }
            }
            
            SyncResponse(
                now = Time.format(now),
                applied = applied,
                conflicts = conflicts,
                changes = serverChanges,
                nextSince = Time.format(now)
            )
        }
    }
    
    fun createNote(userId: UUID, title: String, body: String): Note {
        return transaction {
            Note.new {
                this.userId = EntityID(userId, Users)
                this.title = title
                this.body = body
                this.isDeleted = false
                this.updatedAt = Time.now()
            }
        }
    }
    
    fun updateNote(userId: UUID, noteId: UUID, title: String, body: String): Note? {
        return transaction {
            val note = Note.findById(noteId)
            if (note != null && note.userId.value == userId) {
                note.title = title
                note.body = body
                note.updatedAt = Time.now()
                note
            } else {
                null
            }
        }
    }
    
    fun deleteNote(userId: UUID, noteId: UUID): Boolean {
        return transaction {
            val note = Note.findById(noteId)
            if (note != null && note.userId.value == userId) {
                note.isDeleted = true
                note.updatedAt = Time.now()
                true
            } else {
                false
            }
        }
    }
    
    fun getNote(userId: UUID, noteId: UUID): Note? {
        return transaction {
            val note = Note.findById(noteId)
            if (note != null && note.userId.value == userId) {
                note
            } else {
                null
            }
        }
    }
    
    fun getUserNotes(userId: UUID): List<Note> {
        return transaction {
            Note.find { Notes.userId eq userId }
                .orderBy(Notes.updatedAt to SortOrder.DESC)
                .toList()
        }
    }
}
