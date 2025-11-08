# Sync API Quick Reference

## Endpoint

```
POST /sync
Authorization: Bearer <jwt-token>
Content-Type: application/json
```

## Request Format

```json
{
  "since": "ISO-8601-timestamp-or-null",
  "changes": [
    {
      "id": "uuid",
      "title": "string",
      "body": "string",
      "isDeleted": boolean,
      "updatedAt": "ISO-8601-timestamp"
    }
  ]
}
```

## Response Format

```json
{
  "now": "ISO-8601-timestamp",
  "applied": ["uuid1", "uuid2"],
  "conflicts": [
    {
      "id": "uuid",
      "title": "string",
      "body": "string",
      "isDeleted": boolean,
      "updatedAt": "ISO-8601-timestamp"
    }
  ],
  "changes": [
    {
      "id": "uuid",
      "title": "string",
      "body": "string",
      "isDeleted": boolean,
      "updatedAt": "ISO-8601-timestamp"
    }
  ],
  "nextSince": "ISO-8601-timestamp"
}
```

## Example: First Sync (Get All Notes)

**Request:**
```bash
curl -X POST http://localhost:8080/sync \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "since": null,
    "changes": []
  }'
```

**Response:**
```json
{
  "now": "2025-01-18T10:30:00Z",
  "applied": [],
  "conflicts": [],
  "changes": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "title": "First Note",
      "body": "This is my first note",
      "isDeleted": false,
      "updatedAt": "2025-01-18T09:00:00Z"
    },
    {
      "id": "550e8400-e29b-41d4-a716-446655440001",
      "title": "Second Note",
      "body": "Another note",
      "isDeleted": false,
      "updatedAt": "2025-01-18T09:15:00Z"
    }
  ],
  "nextSince": "2025-01-18T10:30:00Z"
}
```

**Action:** Store `nextSince` value for next sync.

## Example: Sync with New Note

**Request:**
```bash
curl -X POST http://localhost:8080/sync \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "since": "2025-01-18T10:30:00Z",
    "changes": [
      {
        "id": "660e8400-e29b-41d4-a716-446655440002",
        "title": "New Note",
        "body": "Created on mobile",
        "isDeleted": false,
        "updatedAt": "2025-01-18T11:00:00Z"
      }
    ]
  }'
```

**Response:**
```json
{
  "now": "2025-01-18T11:05:00Z",
  "applied": ["660e8400-e29b-41d4-a716-446655440002"],
  "conflicts": [],
  "changes": [],
  "nextSince": "2025-01-18T11:05:00Z"
}
```

**Action:** 
- Mark note `660e8400-e29b-41d4-a716-446655440002` as synced
- Update `lastSyncAt` to `2025-01-18T11:05:00Z`

## Example: Conflict Scenario

**Request:**
```bash
curl -X POST http://localhost:8080/sync \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "since": "2025-01-18T10:30:00Z",
    "changes": [
      {
        "id": "550e8400-e29b-41d4-a716-446655440000",
        "title": "Updated Locally",
        "body": "Client updated this",
        "isDeleted": false,
        "updatedAt": "2025-01-18T10:45:00Z"
      }
    ]
  }'
```

**Response (if server has newer version):**
```json
{
  "now": "2025-01-18T11:05:00Z",
  "applied": [],
  "conflicts": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "title": "Updated on Server",
      "body": "Server has newer version",
      "isDeleted": false,
      "updatedAt": "2025-01-18T11:00:00Z"
    }
  ],
  "changes": [],
  "nextSince": "2025-01-18T11:05:00Z"
}
```

**Action:** 
- Replace local note with server version from `conflicts`
- Update local database with server note

## Example: Server Has Updates

**Request:**
```bash
curl -X POST http://localhost:8080/sync \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "since": "2025-01-18T10:30:00Z",
    "changes": []
  }'
```

**Response:**
```json
{
  "now": "2025-01-18T11:05:00Z",
  "applied": [],
  "conflicts": [],
  "changes": [
    {
      "id": "770e8400-e29b-41d4-a716-446655440003",
      "title": "Note from Desktop",
      "body": "Created on another device",
      "isDeleted": false,
      "updatedAt": "2025-01-18T10:45:00Z"
    }
  ],
  "nextSince": "2025-01-18T11:05:00Z"
}
```

**Action:** 
- Insert/update note from `changes` array
- Update `lastSyncAt`

## Example: Delete Note

**Request:**
```bash
curl -X POST http://localhost:8080/sync \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "since": "2025-01-18T10:30:00Z",
    "changes": [
      {
        "id": "550e8400-e29b-41d4-a716-446655440001",
        "title": "Second Note",
        "body": "Another note",
        "isDeleted": true,
        "updatedAt": "2025-01-18T11:10:00Z"
      }
    ]
  }'
```

**Response:**
```json
{
  "now": "2025-01-18T11:15:00Z",
  "applied": ["550e8400-e29b-41d4-a716-446655440001"],
  "conflicts": [],
  "changes": [],
  "nextSince": "2025-01-18T11:15:00Z"
}
```

**Action:** 
- Mark note as deleted locally (or hide from UI)
- Mark as synced

## Testing with Postman

### Step 1: Get JWT Token
```
POST http://localhost:8080/auth/login
Body: {
  "email": "user@example.com",
  "password": "password123"
}
Response: {
  "accessToken": "...",
  "refreshToken": "..."
}
```

### Step 2: Set Authorization Header
```
Authorization: Bearer <accessToken from step 1>
```

### Step 3: Initial Sync
```
POST http://localhost:8080/sync
Body: {
  "since": null,
  "changes": []
}
```

### Step 4: Sync with Changes
```
POST http://localhost:8080/sync
Body: {
  "since": "2025-01-18T10:30:00Z",
  "changes": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "title": "Test Note",
      "body": "Testing sync",
      "isDeleted": false,
      "updatedAt": "2025-01-18T11:00:00Z"
    }
  ]
}
```

## Common Patterns

### Pattern 1: Initial App Launch
```kotlin
// Check if first sync
if (lastSyncAt == null) {
    syncRequest = SyncRequest(since = null, changes = emptyList())
} else {
    syncRequest = SyncRequest(since = lastSyncAt, changes = getDirtyNotes())
}
```

### Pattern 2: After Local Change
```kotlin
// User creates/updates note
note.isDirty = true
saveNote(note)

// Option 1: Immediate sync (simple)
sync()

// Option 2: Debounced sync (better)
debouncedSync()  // Wait 2 seconds, then sync
```

### Pattern 3: Periodic Background Sync
```kotlin
// Every 5 minutes
timer.schedule(5.minutes) {
    sync()
}
```

### Pattern 4: Network State Change
```kotlin
networkCallback.onAvailable {
    sync()  // Sync when network available
}
```

## Error Handling

### HTTP 401 (Unauthorized)
- Token expired or invalid
- Solution: Refresh token and retry

### HTTP 400 (Bad Request)
- Invalid request format
- Check: JSON structure, timestamp format

### Network Error
- No internet connection
- Solution: Queue changes, retry later

### Sync Error Response
```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Invalid timestamp format",
  "traceId": "uuid"
}
```

## Quick Checklist

- [ ] Get JWT token from `/auth/login`
- [ ] Set Authorization header
- [ ] First sync: use `since: null`
- [ ] Store `nextSince` from response
- [ ] Use stored `nextSince` for subsequent syncs
- [ ] Mark notes as dirty when changed locally
- [ ] Process `applied` list (mark as synced)
- [ ] Process `conflicts` (replace with server version)
- [ ] Process `changes` (insert/update locally)
- [ ] Update `lastSyncAt` with `nextSince`


