# QuickerNotes Server

A production-ready Ktor backend for notes sync and user management with PostgreSQL, featuring offline-first bidirectional sync with Last-Writer-Wins conflict resolution.

## Features

- **User Management**: Sign up, login, refresh tokens, password reset via email
- **Notes Sync**: Bidirectional sync with delta-based updates and LWW conflict resolution
- **Security**: JWT authentication with access/refresh token pattern
- **Database**: PostgreSQL with Exposed ORM and Flyway migrations
- **Email**: SMTP integration for password reset and welcome emails
- **Testing**: Comprehensive test suite with Ktor server tests
- **Monitoring**: Request logging and metrics with Micrometer/Prometheus

## Tech Stack

- **Language**: Kotlin (JVM 21)
- **Framework**: Ktor 3.x (Netty)
- **Database**: PostgreSQL 15+ with Exposed ORM
- **Migrations**: Flyway
- **Auth**: JWT (HS256) with refresh tokens
- **Email**: SMTP (Mailhog for development)
- **Build**: Gradle Kotlin DSL
- **Tests**: JUnit5 + Ktor server tests

## Quick Start

### Prerequisites

- Java 21+
- Docker and Docker Compose
- Gradle (or use the wrapper)

### 1. Start Services

Choose your database preference:

**Option A: PostgreSQL (Default)**
```bash
# Start PostgreSQL and Mailhog
docker-compose --profile postgres up -d db-postgres mailhog

# Wait for services to be ready
docker-compose ps
```

**Option B: MySQL**
```bash
# Start MySQL and Mailhog
docker-compose --profile mysql up -d db-mysql mailhog

# Wait for services to be ready
docker-compose ps
```

### 2. Configure Environment

```bash
# Copy environment template
cp env.sample .env

# Edit .env with your settings (optional for local development)
# The defaults work with the docker-compose setup
```

**Note**: The application will automatically load the `.env` file if it exists. If the file is missing, it will use the default values. You can customize any setting by uncommenting and modifying the values in your `.env` file.

**Database Selection**: To use MySQL instead of PostgreSQL, edit your `.env` file:
```bash
DATABASE_TYPE=mysql
DATABASE_URL=jdbc:mysql://localhost:3306/quickernotes?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
DB_USER=root
DB_PASSWORD=password
```

### 3. Run Database Migrations

```bash
./gradlew flywayMigrate
```

### 4. Start the Server

```bash
./gradlew run
```

The server will start on `http://localhost:8080`

### 5. Verify Setup

```bash
# Health check
curl http://localhost:8080/health

# Version
curl http://localhost:8080/version
```

## API Documentation

### Authentication Endpoints

#### Sign Up
```bash
curl -X POST http://localhost:8080/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123"
  }'
```

Response:
```json
{
  "userId": "uuid",
  "accessToken": "jwt-token",
  "refreshToken": "refresh-token",
  "email": "email"
}
```

#### Login
```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123"
  }'
```

#### Refresh Token
```bash
curl -X POST http://localhost:8080/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "your-refresh-token"
  }'
```

#### Forgot Password
```bash
curl -X POST http://localhost:8080/auth/forgot \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com"
  }'
```

#### Reset Password
```bash
curl -X POST http://localhost:8080/auth/reset \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "code": "123456",
    "newPassword": "newpassword123"
  }'
```

#### Get User Profile
```bash
curl -X GET http://localhost:8080/auth/me \
  -H "Authorization: Bearer your-access-token"
```

#### Logout
```bash
curl -X POST http://localhost:8080/auth/logout \
  -H "Authorization: Bearer your-access-token" \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "your-refresh-token"
  }'
```

### Sync Endpoints

#### Sync Notes
```bash
curl -X POST http://localhost:8080/sync \
  -H "Authorization: Bearer your-access-token" \
  -H "Content-Type: application/json" \
  -d '{
    "since": "2025-01-01T00:00:00Z",
    "changes": [
      {
        "id": "note-uuid",
        "title": "My Note",
        "body": "Note content",
        "isDeleted": false,
        "updatedAt": "2025-01-01T12:00:00Z"
      }
    ]
  }'
```

Response:
```json
{
  "now": "2025-01-01T12:34:56Z",
  "applied": ["note-uuid"],
  "conflicts": [],
  "changes": [
    {
      "id": "server-note-uuid",
      "title": "Server Note",
      "body": "Server content",
      "isDeleted": false,
      "updatedAt": "2025-01-01T12:30:00Z"
    }
  ],
  "nextSince": "2025-01-01T12:34:56Z"
}
```

## Sync Algorithm

The server implements Last-Writer-Wins (LWW) conflict resolution:

1. **New Notes**: If server has no note with the given ID, insert it
2. **Updates**: If client's `updatedAt` > server's `updatedAt`, accept client change
3. **Conflicts**: If server's `updatedAt` >= client's `updatedAt`, reject client change and return server version in `conflicts`
4. **Deletions**: Use `isDeleted: true` flag (soft delete)

### Sync Flow Example

1. **Initial Sync**: Client sends `since: null, changes: []` to get all server notes
2. **Local Changes**: Client makes changes locally and sends them in `changes` array
3. **Conflict Resolution**: Server applies LWW rules and returns `applied`/`conflicts`
4. **Server Updates**: Server returns all notes updated since `since` timestamp
5. **Next Sync**: Client uses `nextSince` for subsequent syncs

## Development

### Running Tests

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests "AuthRoutesTest"

# Run with coverage
./gradlew test jacocoTestReport
```

### Database Management

```bash
# Run migrations
./gradlew flywayMigrate

# Clean database
./gradlew flywayClean

# Check migration status
./gradlew flywayInfo
```

### Email Testing

With Mailhog running, emails are captured at `http://localhost:8025`

## Configuration

### Environment Variables

The application uses a `.env` file for configuration. Copy `env.sample` to `.env` and customize as needed:

| Variable | Default | Description |
|----------|---------|-------------|
| `DATABASE_TYPE` | `postgresql` | Database type: `postgresql` or `mysql` |
| `DATABASE_URL` | See below | Database connection URL |
| `DB_USER` | See below | Database username |
| `DB_PASSWORD` | `password` | Database password |
| `JWT_SECRET` | `default-secret-change-in-production` | JWT signing secret |
| `ACCESS_TOKEN_TTL_MIN` | `15` | Access token expiry (minutes) |
| `REFRESH_TOKEN_TTL_DAYS` | `30` | Refresh token expiry (days) |
| `SMTP_HOST` | `localhost` | SMTP server host |
| `SMTP_PORT` | `1025` | SMTP server port |
| `SMTP_USER` | - | SMTP username (optional) |
| `SMTP_PASS` | - | SMTP password (optional) |
| `EMAIL_FROM` | `noreply@quickernotes.local` | From email address |
| `APP_BASE_URL` | `http://localhost:8080` | Base URL for links |
| `PORT` | `8080` | Server port |

**Default Database URLs:**
- PostgreSQL: `jdbc:postgresql://localhost:5432/quickernotes`
- MySQL: `jdbc:mysql://localhost:3306/quickernotes?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true`

**Default Database Users:**
- PostgreSQL: `postgres`
- MySQL: `root`

**Example .env file for PostgreSQL:**
```bash
# Database Configuration
DATABASE_TYPE=postgresql
DATABASE_URL=jdbc:postgresql://localhost:5432/quickernotes
DB_USER=postgres
DB_PASSWORD=password

# JWT Configuration
JWT_SECRET=your-super-secret-jwt-key-change-this-in-production
ACCESS_TOKEN_TTL_MIN=15
REFRESH_TOKEN_TTL_DAYS=30

# Email Configuration (for development with Mailhog)
SMTP_HOST=localhost
SMTP_PORT=1025
SMTP_USER=
SMTP_PASS=
EMAIL_FROM=noreply@quickernotes.local

# Application Configuration
APP_BASE_URL=http://localhost:8080
PORT=8080
```

**Example .env file for MySQL:**
```bash
# Database Configuration
DATABASE_TYPE=mysql
DATABASE_URL=jdbc:mysql://localhost:3306/quickernotes?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
DB_USER=root
DB_PASSWORD=password

# JWT Configuration
JWT_SECRET=your-super-secret-jwt-key-change-this-in-production
ACCESS_TOKEN_TTL_MIN=15
REFRESH_TOKEN_TTL_DAYS=30

# Email Configuration (for development with Mailhog)
SMTP_HOST=localhost
SMTP_PORT=1025
SMTP_USER=
SMTP_PASS=
EMAIL_FROM=noreply@quickernotes.local

# Application Configuration
APP_BASE_URL=http://localhost:8080
PORT=8080
```

### Production Deployment

1. **Database**: Use managed PostgreSQL or MySQL service
2. **Email**: Configure SMTP with production email service
3. **Security**: Set strong `JWT_SECRET` and use HTTPS
4. **Monitoring**: Set up Prometheus metrics collection
5. **Logging**: Configure structured logging for production

## Architecture

```
src/main/kotlin/
├── config/           # Configuration classes
├── data/             # Database setup and migrations
├── domain/           # Models and DTOs
│   ├── models/       # Exposed ORM entities
│   └── dto/          # Request/response DTOs
├── plugins/          # Ktor plugins
├── routes/           # API route handlers
├── services/         # Business logic
└── util/             # Utilities (hashing, time, errors)
```

## Error Handling

The API uses RFC 7807 Problem Details format for errors:

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Invalid request data",
  "traceId": "uuid"
}
```

## License

MIT License - see LICENSE file for details.