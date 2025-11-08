# Client-Side Sync Implementation Guide

This guide shows you how to implement the sync mechanism on the client side (Kotlin/Android).

## Architecture Overview

```
┌─────────────────────────────────────────┐
│         Client App (Android)            │
│  ┌───────────────────────────────────┐ │
│  │      UI Layer                     │ │
│  │  (Compose UI, ViewModels)         │ │
│  └──────────────┬────────────────────┘ │
│                 │                        │
│                 ▼                        │
│  ┌───────────────────────────────────┐ │
│  │    Sync Manager                   │ │
│  │  - Coordinates sync               │ │
│  │  - Handles conflicts              │ │
│  │  - Manages sync state             │ │
│  └──────────────┬────────────────────┘ │
│                 │                        │
│         ┌───────┴───────┐                │
│         │               │                │
│         ▼               ▼                │
│  ┌──────────┐   ┌──────────────┐        │
│  │  Local   │   │  Network     │        │
│  │ Database │   │  API Client  │        │
│  │  (Room)  │   │  (Ktor)      │        │
│  └──────────┘   └──────────────┘        │
└─────────────────────────────────────────┘
```

## Step-by-Step Implementation

### 1. Local Database Schema (Room)

```kotlin
@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey
    val id: String,  // UUID as String
    
    val title: String,
    val body: String,
    val isDeleted: Boolean = false,
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: String,  // ISO 8601 timestamp
    
    @ColumnInfo(name = "is_dirty")
    val isDirty: Boolean = false,  // Needs sync?
    
    @ColumnInfo(name = "is_conflict")
    val isConflict: Boolean = false
)

@Entity(tableName = "sync_state")
data class SyncState(
    @PrimaryKey
    val id: Int = 1,
    
    @ColumnInfo(name = "last_sync_at")
    val lastSyncAt: String? = null  // ISO 8601 timestamp
)
```

### 2. API Client Interface

```kotlin
interface SyncApi {
    suspend fun sync(request: SyncRequest): SyncResponse
}

data class SyncRequest(
    val since: String? = null,
    val changes: List<NoteChange>
)

data class SyncResponse(
    val now: String,
    val applied: List<String>,
    val conflicts: List<NoteChange>,
    val changes: List<NoteChange>,
    val nextSince: String
)

data class NoteChange(
    val id: String,
    val title: String,
    val body: String,
    val isDeleted: Boolean,
    val updatedAt: String
)
```

### 3. Ktor API Implementation

```kotlin
class SyncApiImpl(
    private val httpClient: HttpClient,
    private val baseUrl: String
) : SyncApi {
    
    override suspend fun sync(request: SyncRequest): SyncResponse {
        return httpClient.post("$baseUrl/sync") {
            header("Authorization", "Bearer ${getAccessToken()}")
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }
    
    private fun getAccessToken(): String {
        // Get from secure storage
        return tokenStorage.getAccessToken()
    }
}
```

### 4. Sync Manager

```kotlin
class SyncManager(
    private val noteDao: NoteDao,
    private val syncStateDao: SyncStateDao,
    private val syncApi: SyncApi,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    
    sealed class SyncResult {
        object Success : SyncResult()
        data class Error(val message: String) : SyncResult()
        data class Conflict(val noteIds: List<String>) : SyncResult()
    }
    
    suspend fun performSync(): SyncResult = withContext(dispatcher) {
        try {
            // 1. Get last sync timestamp
            val lastSync = syncStateDao.getSyncState()?.lastSyncAt
            
            // 2. Get all dirty notes (local changes)
            val localChanges = noteDao.getDirtyNotes().map { note ->
                NoteChange(
                    id = note.id,
                    title = note.title,
                    body = note.body,
                    isDeleted = note.isDeleted,
                    updatedAt = note.updatedAt
                )
            }
            
            // 3. Build sync request
            val request = SyncRequest(
                since = lastSync,
                changes = localChanges
            )
            
            // 4. Call sync API
            val response = syncApi.sync(request)
            
            // 5. Process response
            processSyncResponse(response)
            
            SyncResult.Success
            
        } catch (e: Exception) {
            Log.e("SyncManager", "Sync failed", e)
            SyncResult.Error(e.message ?: "Unknown error")
        }
    }
    
    private suspend fun processSyncResponse(response: SyncResponse) {
        // 1. Mark applied notes as synced
        response.applied.forEach { noteId ->
            noteDao.markAsSynced(noteId)
        }
        
        // 2. Handle conflicts - replace local with server
        response.conflicts.forEach { serverNote ->
            noteDao.insertOrUpdate(
                NoteEntity(
                    id = serverNote.id,
                    title = serverNote.title,
                    body = serverNote.body,
                    isDeleted = serverNote.isDeleted,
                    updatedAt = serverNote.updatedAt,
                    isDirty = false,  // Server version is authoritative
                    isConflict = false
                )
            )
        }
        
        // 3. Apply server changes
        response.changes.forEach { serverNote ->
            noteDao.insertOrUpdate(
                NoteEntity(
                    id = serverNote.id,
                    title = serverNote.title,
                    body = serverNote.body,
                    isDeleted = serverNote.isDeleted,
                    updatedAt = serverNote.updatedAt,
                    isDirty = false,
                    isConflict = false
                )
            )
        }
        
        // 4. Update last sync timestamp
        syncStateDao.updateSyncState(
            SyncState(
                id = 1,
                lastSyncAt = response.nextSince
            )
        )
    }
    
    // Initial sync - get all notes from server
    suspend fun performInitialSync(): SyncResult = withContext(dispatcher) {
        try {
            val request = SyncRequest(
                since = null,  // null = get everything
                changes = emptyList()
            )
            
            val response = syncApi.sync(request)
            
            // Apply all server notes
            response.changes.forEach { serverNote ->
                noteDao.insertOrUpdate(
                    NoteEntity(
                        id = serverNote.id,
                        title = serverNote.title,
                        body = serverNote.body,
                        isDeleted = serverNote.isDeleted,
                        updatedAt = serverNote.updatedAt,
                        isDirty = false,
                        isConflict = false
                    )
                )
            }
            
            // Update sync state
            syncStateDao.updateSyncState(
                SyncState(
                    id = 1,
                    lastSyncAt = response.nextSince
                )
            )
            
            SyncResult.Success
            
        } catch (e: Exception) {
            Log.e("SyncManager", "Initial sync failed", e)
            SyncResult.Error(e.message ?: "Unknown error")
        }
    }
}
```

### 5. Repository Layer

```kotlin
class NoteRepository(
    private val noteDao: NoteDao,
    private val syncManager: SyncManager
) {
    
    suspend fun getAllNotes(): Flow<List<NoteEntity>> {
        return noteDao.getAllNotes()
    }
    
    suspend fun getNote(id: String): NoteEntity? {
        return noteDao.getNote(id)
    }
    
    suspend fun createNote(title: String, body: String): NoteEntity {
        val note = NoteEntity(
            id = UUID.randomUUID().toString(),
            title = title,
            body = body,
            isDeleted = false,
            updatedAt = Instant.now().toString(),  // Client timestamp
            isDirty = true,  // Mark as needing sync
            isConflict = false
        )
        
        noteDao.insert(note)
        
        // Optionally sync immediately (or batch sync later)
        syncManager.performSync()
        
        return note
    }
    
    suspend fun updateNote(id: String, title: String, body: String) {
        noteDao.updateNote(
            id = id,
            title = title,
            body = body,
            updatedAt = Instant.now().toString(),
            isDirty = true  // Mark as dirty
        )
        
        // Optionally sync immediately
        syncManager.performSync()
    }
    
    suspend fun deleteNote(id: String) {
        noteDao.markAsDeleted(id, Instant.now().toString(), isDirty = true)
        syncManager.performSync()
    }
    
    suspend fun sync() {
        syncManager.performSync()
    }
}
```

### 6. ViewModel Integration

```kotlin
class NotesViewModel(
    private val repository: NoteRepository
) : ViewModel() {
    
    val notes: Flow<List<NoteEntity>> = repository.getAllNotes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    val syncState = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    
    init {
        // Perform initial sync when ViewModel is created
        viewModelScope.launch {
            performInitialSync()
        }
        
        // Setup periodic sync
        setupPeriodicSync()
    }
    
    fun createNote(title: String, body: String) {
        viewModelScope.launch {
            repository.createNote(title, body)
        }
    }
    
    fun updateNote(id: String, title: String, body: String) {
        viewModelScope.launch {
            repository.updateNote(id, title, body)
        }
    }
    
    fun deleteNote(id: String) {
        viewModelScope.launch {
            repository.deleteNote(id)
        }
    }
    
    fun syncNow() {
        viewModelScope.launch {
            syncState.value = SyncStatus.Syncing
            repository.sync()
            syncState.value = SyncStatus.Success
        }
    }
    
    private suspend fun performInitialSync() {
        // Check if this is first sync
        // If yes, call initial sync API
        // Otherwise, regular sync
        syncState.value = SyncStatus.Syncing
        repository.sync()
        syncState.value = SyncStatus.Success
    }
    
    private fun setupPeriodicSync() {
        viewModelScope.launch {
            while (true) {
                delay(5.minutes)  // Sync every 5 minutes
                repository.sync()
            }
        }
    }
    
    sealed class SyncStatus {
        object Idle : SyncStatus()
        object Syncing : SyncStatus()
        object Success : SyncStatus()
        data class Error(val message: String) : SyncStatus()
    }
}
```

### 7. Network State Monitoring

```kotlin
class NetworkSyncWorker(
    context: Context,
    params: WorkerParameters,
    private val syncManager: SyncManager
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        return try {
            syncManager.performSync()
            Result.success()
        } catch (e: Exception) {
            Result.retry()  // Retry on failure
        }
    }
}

// Schedule periodic sync
class SyncScheduler(private val context: Context) {
    
    fun schedulePeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val syncRequest = PeriodicWorkRequestBuilder<NetworkSyncWorker>(
            15, TimeUnit.MINUTES  // Sync every 15 minutes
        )
            .setConstraints(constraints)
            .build()
        
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                "sync",
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
    }
}
```

### 8. Conflict Resolution UI

```kotlin
@Composable
fun ConflictResolutionDialog(
    conflictNote: NoteChange,
    onResolve: (NoteChange) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Conflict Detected") },
        text = {
            Column {
                Text("This note was updated on another device.")
                Text("Server version:")
                Text(conflictNote.title, fontWeight = FontWeight.Bold)
                Text(conflictNote.body)
            }
        },
        confirmButton = {
            Button(onClick = { onResolve(conflictNote) }) {
                Text("Use Server Version")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Keep Local (Will Conflict Again)")
            }
        }
    )
}
```

## Complete Flow Example

### Scenario: User creates a note while offline

```kotlin
// 1. User creates note offline
viewModel.createNote("My Note", "Content")

// Local database:
// - Note inserted with isDirty = true
// - User sees note immediately in UI

// 2. Network becomes available
// Network callback triggers sync

// 3. Sync process
val request = SyncRequest(
    since = lastSyncTimestamp,
    changes = [
        NoteChange(
            id = "local-uuid",
            title = "My Note",
            body = "Content",
            isDeleted = false,
            updatedAt = "2025-01-01T12:00:00Z"
        )
    ]
)

// Server processes:
// - Note doesn't exist → INSERT
// - Add to "applied" list
// - Return server timestamp

// 4. Process response
// - Mark note as synced (isDirty = false)
// - Update updatedAt with server timestamp
// - Update lastSyncAt

// 5. Next sync
// - Note is not dirty
// - Won't be sent in changes
// - Only new changes will sync
```

## Testing

### Unit Tests

```kotlin
@Test
fun `test sync applies client changes`() = runTest {
    // Setup
    val localNote = createLocalNote(isDirty = true)
    val mockResponse = SyncResponse(
        now = Instant.now().toString(),
        applied = listOf(localNote.id),
        conflicts = emptyList(),
        changes = emptyList(),
        nextSince = Instant.now().toString()
    )
    
    // Execute
    syncManager.processSyncResponse(mockResponse)
    
    // Verify
    val note = noteDao.getNote(localNote.id)
    assertFalse(note?.isDirty ?: true)
}
```

### Integration Tests

```kotlin
@Test
fun `test end-to-end sync`() = runTest {
    // 1. Create note locally
    repository.createNote("Test", "Content")
    
    // 2. Sync
    val result = syncManager.performSync()
    
    // 3. Verify
    assertEquals(SyncResult.Success, result)
    
    // 4. Verify note is synced
    val note = noteDao.getNote(noteId)
    assertFalse(note?.isDirty ?: true)
}
```

## Best Practices Summary

1. **Always mark notes as dirty** when changed locally
2. **Use server timestamp** (`nextSince`) for subsequent syncs
3. **Handle conflicts** by replacing local with server version
4. **Batch syncs** - don't sync on every keystroke
5. **Show sync status** to users
6. **Handle offline gracefully** - queue changes
7. **Retry failed syncs** with exponential backoff
8. **Test thoroughly** with multiple devices


