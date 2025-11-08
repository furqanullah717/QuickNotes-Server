# Sync Flow Documentation

## Overview

The QuickerNotes server implements an **offline-first bidirectional sync** mechanism with **Last-Writer-Wins (LWW)** conflict resolution. This allows clients to work offline and sync changes when they come back online.

## Key Concepts

### 1. **Last-Writer-Wins (LWW) Conflict Resolution**
- The server's `updatedAt` timestamp is the **source of truth**
- If a client's `updatedAt` is **newer** than the server's → client change wins
- If the server's `updatedAt` is **newer or equal** → server version wins (conflict)
- Conflicts are returned to the client so they can resolve them

### 2. **Delta-Based Sync**
- Only syncs changes since the last sync (using `since` timestamp)
- Reduces bandwidth and improves performance
- First sync sends all notes (when `since` is null)

### 3. **Soft Deletes**
- Notes are never hard-deleted
- `isDeleted: true` marks a note as deleted
- Deleted notes are synced like regular notes

## Sync Flow Diagram

```
┌─────────────┐
│   Client    │
└──────┬──────┘
       │
       │ POST /sync
       │ {
       │   "since": "2025-01-01T12:00:00Z",
       │   "changes": [
       │     { "id": "...", "title": "...", "body": "...", "isDeleted": false, "updatedAt": "..." }
       │   ]
       │ }
       ▼
┌─────────────────────────────────────────────────────┐
│                    Server                            │
│  ┌──────────────────────────────────────────────┐  │
│  │  1. Extract userId from JWT                  │  │
│  │  2. Parse "since" timestamp (or null)        │  │
│  │  3. Get current server time (now)            │  │
│  └──────────────────────────────────────────────┘  │
│                           │                          │
│                           ▼                          │
│  ┌──────────────────────────────────────────────┐  │
│  │  APPLY CLIENT CHANGES (LWW Logic)            │  │
│  │  ──────────────────────────────────────     │  │
│  │  For each client change:                     │  │
│  │                                               │  │
│  │  • Note doesn't exist?                       │  │
│  │    → INSERT new note                         │  │
│  │    → Add to "applied"                        │  │
│  │                                               │  │
│  │  • Note exists:                              │  │
│  │    → Compare timestamps                      │  │
│  │    → Client newer? UPDATE → "applied"        │  │
│  │    → Server newer? → "conflicts"             │  │
│  └──────────────────────────────────────────────┘  │
│                           │                          │
│                           ▼                          │
│  ┌──────────────────────────────────────────────┐  │
│  │  COLLECT SERVER CHANGES                      │  │
│  │  ──────────────────────────────────────     │  │
│  │  If since is null:                           │  │
│  │    → Get ALL user notes                      │  │
│  │  Else:                                       │  │
│  │    → Get notes where updatedAt > since       │  │
│  │  → Limit 1000 notes                          │  │
│  └──────────────────────────────────────────────┘  │
│                           │                          │
│                           ▼                          │
│  ┌──────────────────────────────────────────────┐  │
│  │  BUILD RESPONSE                              │  │
│  │  {                                            │  │
│  │    "now": "2025-01-01T12:30:00Z",            │  │
│  │    "applied": ["uuid1", "uuid2"],            │  │
│  │    "conflicts": [...],                       │  │
│  │    "changes": [...],                         │  │
│  │    "nextSince": "2025-01-01T12:30:00Z"       │  │
│  │  }                                            │  │
│  └──────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────┘
       │
       │ Response
       ▼
┌─────────────┐
│   Client    │
│  ─────────  │
│ 1. Update local notes from "applied" list        │
│ 2. Handle "conflicts" (replace local with server)│
│ 3. Insert/update notes from "changes"            │
│ 4. Store "nextSince" for next sync              │
└─────────────┘
```

## API Endpoint

### POST `/sync`

**Authentication:** Required (JWT Bearer token)

**Request:**
```json
{
  "since": "2025-01-01T12:00:00Z",  // ISO 8601 timestamp, nullable
  "changes": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "title": "My Note",
      "body": "Note content here",
      "isDeleted": false,
      "updatedAt": "2025-01-01T12:15:00Z"
    }
  ]
}
```

**Response:**
```json
{
  "now": "2025-01-01T12:30:00Z",
  "applied": [
    "550e8400-e29b-41d4-a716-446655440000"
  ],
  "conflicts": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440001",
      "title": "Server Version",
      "body": "This note was updated on server",
      "isDeleted": false,
      "updatedAt": "2025-01-01T12:20:00Z"
    }
  ],
  "changes": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440002",
      "title": "Note from another device",
      "body": "Created on phone",
      "isDeleted": false,
      "updatedAt": "2025-01-01T12:25:00Z"
    }
  ],
  "nextSince": "2025-01-01T12:30:00Z"
}
```

## Response Fields Explained

### `now`
- Current server timestamp (ISO 8601)
- Authoritative time for this sync operation
- Use this to update client's local time if needed

### `applied`
- List of note IDs that were successfully applied to the server
- These are the client changes that were accepted
- Client should mark these as "synced" locally

### `conflicts`
- List of server notes that conflicted with client changes
- These are notes where the server version is newer
- Client should **replace** local version with server version

### `changes`
- List of server notes that were updated since `since` timestamp
- These are changes from server that client doesn't have
- Client should insert/update these notes locally

### `nextSince`
- Timestamp to use for the next sync request
- Usually equals `now`
- Store this value locally for subsequent syncs

## Client Implementation Guide

### Initial Sync (First Time)

```kotlin
// First sync - get all notes from server
val request = SyncRequest(
    since = null,  // null means "get everything"
    changes = emptyList()  // no local changes yet
)

val response = syncApi.post("/sync", request)

// Process response
for (noteChange in response.changes) {
    localDatabase.insertOrUpdateNote(noteChange)
}

// Store nextSince for future syncs
localDatabase.saveLastSyncTimestamp(response.nextSince)
```

### Regular Sync (After Initial Sync)

```kotlin
// Get last sync timestamp
val lastSync = localDatabase.getLastSyncTimestamp()

// Collect all local changes since last sync
val localChanges = localDatabase.getChangedNotesSince(lastSync)

// Send sync request
val request = SyncRequest(
    since = lastSync,
    changes = localChanges.map { note ->
        NoteChange(
            id = note.id.toString(),
            title = note.title,
            body = note.body,
            isDeleted = note.isDeleted,
            updatedAt = note.updatedAt.toString()
        )
    }
)

val response = syncApi.post("/sync", request)

// 1. Process applied changes
for (appliedId in response.applied) {
    localDatabase.markAsSynced(appliedId)
}

// 2. Handle conflicts (replace local with server)
for (conflictNote in response.conflicts) {
    localDatabase.replaceNote(conflictNote)  // Server wins!
}

// 3. Apply server changes
for (serverNote in response.changes) {
    localDatabase.insertOrUpdateNote(serverNote)
}

// 4. Update last sync timestamp
localDatabase.saveLastSyncTimestamp(response.nextSince)
```

### Handling Local Changes

When a user creates/updates/deletes a note locally:

```kotlin
// User creates a note
val newNote = Note(
    id = UUID.randomUUID(),
    title = "New Note",
    body = "Content",
    isDeleted = false,
    updatedAt = Instant.now(),  // Client's best guess
    isDirty = true  // Mark as unsynced
)

localDatabase.insertNote(newNote)

// Note: Don't sync immediately - batch syncs are more efficient
// Sync periodically or when app comes online
```

## Sync Strategies

### 1. **Periodic Sync**
- Sync every N minutes (e.g., every 5 minutes)
- Good for apps that are usually online

### 2. **Event-Based Sync**
- Sync when app comes to foreground
- Sync when network becomes available
- Sync after local changes (with debouncing)

### 3. **Manual Sync**
- User-initiated sync
- "Pull to refresh" pattern

### 4. **Hybrid Approach** (Recommended)
- Immediate sync for critical changes
- Periodic background sync
- Sync on network availability

## Conflict Resolution Details

### Example Scenario

**Initial State:**
- Server: Note A (updatedAt: 2025-01-01T12:00:00Z)
- Client: Note A (updatedAt: 2025-01-01T12:00:00Z)

**Timeline:**
1. **12:05** - Client updates Note A locally
2. **12:10** - Server updates Note A (from another device)
3. **12:15** - Client syncs

**Sync Result:**
```json
{
  "applied": [],  // Client change rejected
  "conflicts": [
    {
      "id": "note-a-id",
      "title": "Server Version",  // Server's version
      "body": "Updated on server",
      "updatedAt": "2025-01-01T12:10:00Z"
    }
  ]
}
```

**Client Action:**
- Replace local Note A with server version
- Notify user if desired: "Note was updated on another device"

## Edge Cases

### 1. **Concurrent Edits**
- LWW ensures consistent state
- Last update wins (by timestamp)
- User sees latest version on next sync

### 2. **Deleted Notes**
- Use `isDeleted: true` flag
- Deleted notes still sync
- Client can hide deleted notes from UI
- Optionally purge old deleted notes (>30 days)

### 3. **Network Failures**
- Retry sync with exponential backoff
- Store changes locally until sync succeeds
- Show sync status to user

### 4. **Large Number of Notes**
- Server limits to 1000 notes per sync
- If you have >1000 changes, sync multiple times
- Use pagination or cursor-based sync if needed

## Best Practices

### Client Side

1. **Store `nextSince` timestamp**
   - Persist after each successful sync
   - Use for next sync request

2. **Track dirty notes**
   - Mark notes as "dirty" when changed locally
   - Only send dirty notes in sync request
   - Clear dirty flag after sync succeeds

3. **Handle conflicts gracefully**
   - Replace local with server version
   - Optionally notify user of conflicts
   - Consider showing conflict resolution UI

4. **Batch syncs**
   - Don't sync on every keystroke
   - Debounce sync requests
   - Batch multiple changes

5. **Offline support**
   - Queue changes when offline
   - Sync when network available
   - Show sync status indicator

### Server Side

1. **Transactional operations**
   - All sync operations in a transaction
   - Ensures data consistency
   - Atomic updates

2. **Timestamp authority**
   - Server time is authoritative
   - Always use server `now` for updates
   - Client timestamps are only for comparison

3. **Performance**
   - Index on (userId, updatedAt)
   - Limit result sets (currently 1000)
   - Efficient queries

## Testing the Sync

### Using cURL

```bash
# 1. Login first
TOKEN=$(curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}' \
  | jq -r '.accessToken')

# 2. Initial sync
curl -X POST http://localhost:8080/sync \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "since": null,
    "changes": []
  }'

# 3. Sync with changes
curl -X POST http://localhost:8080/sync \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "since": "2025-01-01T12:00:00Z",
    "changes": [
      {
        "id": "550e8400-e29b-41d4-a716-446655440000",
        "title": "My Note",
        "body": "Content here",
        "isDeleted": false,
        "updatedAt": "2025-01-01T12:15:00Z"
      }
    ]
  }'
```

### Using Postman

1. Create a new POST request to `http://localhost:8080/sync`
2. Add Authorization header: `Bearer <your-jwt-token>`
3. Set Content-Type: `application/json`
4. Body:
```json
{
  "since": null,
  "changes": []
}
```

## Common Issues & Solutions

### Issue: Notes not syncing
- **Check**: Is `since` timestamp correct?
- **Check**: Are notes marked as dirty?
- **Check**: Network connectivity
- **Check**: JWT token validity

### Issue: Conflicts every sync
- **Check**: Client and server clocks are synchronized
- **Check**: Are you updating `updatedAt` on client correctly?
- **Solution**: Use server's `now` timestamp after sync

### Issue: Duplicate notes
- **Cause**: Not handling `applied` list correctly
- **Solution**: Mark notes as synced only if in `applied` list

### Issue: Deleted notes reappearing
- **Cause**: Not syncing `isDeleted` flag
- **Solution**: Include deleted notes in sync with `isDeleted: true`

## Next Steps

1. Implement client-side sync logic
2. Add conflict resolution UI
3. Implement offline queue
4. Add sync status indicators
5. Test with multiple devices
6. Monitor sync performance


