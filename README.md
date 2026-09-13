# University Management System

A production-style REST API for managing university departments, programs, courses, enrollment, and more — built as a portfolio project demonstrating backend engineering with Java and Spring Boot.

## Tech Stack

- **Java 25** (LTS)
- **Spring Boot 4.1.1** (Web MVC, Data JPA, Validation, Actuator)
- **PostgreSQL 18** (via Docker)
- **Hibernate 7** (ORM)
- **Flyway** (database migrations)
- **JUnit 5 + Mockito** (unit tests)
- **Testcontainers** (PostgreSQL integration tests)
- **Maven** (build tool with Maven Wrapper)

## Prerequisites

- **Java 25+** — verify with `java -version`
- **Docker Desktop** — required for the local PostgreSQL database and integration tests
- **Git** — for cloning the repository

## Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/BoDe-117/university-management-system.git
cd university-management-system
```

### 2. Start PostgreSQL

```bash
docker compose up -d
```

This starts a PostgreSQL 18 container with:
- Database: `ums_dev`
- Username: `ums_user`
- Password: `ums_pass`
- Port: `5432`

These are local development defaults — not production credentials.

### 3. Run the application

```bash
./mvnw spring-boot:run
```

On Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

The application starts on **port 8081**. Verify it's running:

http://localhost:8081/actuator/health


### 4. Run tests

```bash
./mvnw test
```

This runs unit tests (Mockito), controller tests (MockMvc), and integration tests (Testcontainers — requires Docker to be running).

## API Endpoints

### Departments

| Method | URL | Description | Success |
|--------|-----|-------------|---------|
| `POST` | `/api/v1/departments` | Create a department | `201 Created` |
| `GET` | `/api/v1/departments/{id}` | Get department by ID | `200 OK` |
| `GET` | `/api/v1/departments` | List all departments | `200 OK` |

### Example: Create a department

**Request:**

```json
POST /api/v1/departments
Content-Type: application/json

{
  "code": "CS",
  "name": "Computer Science"
}
```

**Response (201 Created):**

```json
{
  "id": "bef682f0-7009-4b6a-88b9-0b31dda41a9f",
  "code": "CS",
  "name": "Computer Science",
  "createdAt": "2026-09-03T12:58:08.879Z",
  "updatedAt": "2026-09-03T12:58:08.879Z"
}
```

### Error Responses

All errors return a consistent shape:

```json
{
  "timestamp": "2026-09-03T13:01:47.242Z",
  "status": 400,
  "error": "Validation failed",
  "fieldErrors": {
    "code": "Department code is required"
  }
}
```

| Status | Meaning |
|--------|---------|
| `400` | Validation failure, malformed JSON, or invalid UUID |
| `404` | Resource not found |
| `409` | Duplicate code conflict |

## Project Structure

src/main/java/.../
├── common/exception/ # Global exception handling
├── department/
│ ├── controller/ # REST endpoints
│ ├── dto/ # Request/response objects
│ ├── entity/ # JPA entity
│ ├── exception/ # Domain-specific exceptions
│ ├── repository/ # Data access
│ └── service/ # Business logic
└── UniversityManagementSystemApplication.java


## Testing Strategy

- **Unit tests** — Service logic with Mockito (mocked repository)
- **Controller tests** — HTTP contract verification with MockMvc
- **Integration tests** — Full stack against real PostgreSQL via Testcontainers, including concurrency race condition verification

## License

This project is for educational and portfolio purposes.
