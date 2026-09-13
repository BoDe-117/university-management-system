# University Management System

A university management REST API built as a portfolio project using Java, Spring Boot, and PostgreSQL.

Department management is currently implemented. Program management, courses, and enrollment are planned.

## Current Features

- Create a department.
- Retrieve a department by ID.
- List all departments.
- Validate required fields and field lengths.
- Enforce unique department codes in PostgreSQL.
- Translate duplicate-code conflicts into HTTP `409`, including concurrent creation attempts.
- Return structured errors for supported Department validation and lookup failures.

## Tech Stack

- Java 25 target
- Spring Boot 4.1.1
- Spring Web MVC
- Spring Data JPA and Hibernate
- PostgreSQL 18
- Flyway
- JUnit and Mockito
- MockMvc
- Testcontainers 2
- Maven with Maven Wrapper
- GitHub Actions

## Prerequisites

- JDK 25 to match the CI configuration.
- Docker Desktop, or a compatible Docker environment, running Linux containers.
- Git.

Check the Java version Maven actually uses:

```powershell
.\mvnw.cmd -version
```

On Linux or macOS:

```bash
./mvnw -version
```

The project targets Java 25. Maven's runtime JDK is determined by your local environment, including `JAVA_HOME`.

## Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/BoDe-117/university-management-system.git
cd university-management-system
```

### 2. Start the development database

```bash
docker compose up -d
```

The development database uses:

| Setting | Value |
|---------|-------|
| PostgreSQL image | `postgres:18` |
| Database | `ums_dev` |
| Username | `ums_user` |
| Password | `ums_pass` |
| Host port | `5432` |

These credentials are local development defaults. Do not reuse them for a production deployment.

Flyway applies database migrations when the application starts. Hibernate validates that the database schema matches the entity mappings.

### 3. Run the application

Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

Linux or macOS:

```bash
./mvnw spring-boot:run
```

The application runs on port `8081`.

Open the health endpoint:

[http://localhost:8081/actuator/health](http://localhost:8081/actuator/health)

A healthy application returns:

```json
{
  "status": "UP"
}
```

## Running Tests

Keep Docker running for the integration tests.

Windows PowerShell:

```powershell
.\mvnw.cmd clean test
```

Linux or macOS:

```bash
./mvnw clean test
```

This runs the service, controller, and PostgreSQL integration tests.

Integration tests start an isolated PostgreSQL container with a dynamically assigned host port. They do not require the development database started by `docker compose up -d`.

### Run individual test classes

Windows PowerShell:

```powershell
.\mvnw.cmd "-Dtest=DepartmentServiceTest" test
.\mvnw.cmd "-Dtest=DepartmentControllerTest" test
.\mvnw.cmd "-Dtest=DepartmentIntegrationTest" test
```

Linux or macOS:

```bash
./mvnw -Dtest=DepartmentServiceTest test
./mvnw -Dtest=DepartmentControllerTest test
./mvnw -Dtest=DepartmentIntegrationTest test
```

Service and controller tests do not require Docker.

### Testing Strategy

- **Service unit tests:** verify business rules, response mapping, and exception handling using a mocked repository.
- **Controller tests:** verify HTTP status codes, the creation `Location` header, response bodies, and invalid request handling using MockMvc.
- **Integration tests:** verify application startup, migrations, persistence, and duplicate handling against PostgreSQL through the Spring-managed service.

The concurrency test coordinates two requests so both observe an unused code before attempting insertion. It checks that one succeeds, one produces a domain conflict, and only one row is stored.

A duplicate-key warning in the database logs is expected during that test.

## Continuous Integration

The GitHub Actions workflow is configured to run the Maven test suite on:

- Pull requests targeting `main`.
- Pushes to `main`.

CI uses Java 25 on an Ubuntu runner.

Check the [Actions page](https://github.com/BoDe-117/university-management-system/actions) for the latest run results.

## Department API

Base URL: `http://localhost:8081`

| Method | Endpoint | Description | Success |
|--------|----------|-------------|---------|
| `POST` | `/api/v1/departments` | Create a department | `201 Created` |
| `GET` | `/api/v1/departments/{id}` | Retrieve a department | `200 OK` |
| `GET` | `/api/v1/departments` | List departments | `200 OK` |

### Request Validation

| Field | Rules |
|-------|-------|
| `code` | Required, not blank, maximum 20 characters |
| `name` | Required, not blank, maximum 150 characters |

Department codes must be unique. The current implementation stores codes as supplied, without trimming or case normalization.

### Create a Department

```http
POST /api/v1/departments HTTP/1.1
Host: localhost:8081
Content-Type: application/json

{
  "code": "CS",
  "name": "Computer Science"
}
```

Example response headers:

```http
HTTP/1.1 201 Created
Location: /api/v1/departments/bef682f0-7009-4b6a-88b9-0b31dda41a9f
Content-Type: application/json
```

Example response body:

```json
{
  "id": "bef682f0-7009-4b6a-88b9-0b31dda41a9f",
  "code": "CS",
  "name": "Computer Science",
  "createdAt": "2026-09-03T12:58:08.879Z",
  "updatedAt": "2026-09-03T12:58:08.879Z"
}
```

IDs and timestamps are generated; actual values will differ.

### Retrieve a Department

Use the ID returned by the creation endpoint:

```http
GET /api/v1/departments/bef682f0-7009-4b6a-88b9-0b31dda41a9f HTTP/1.1
Host: localhost:8081
```

An existing department returns `200` with a department response object. A missing department returns `404`.

### List Departments

```http
GET /api/v1/departments HTTP/1.1
Host: localhost:8081
```

Returns `200` with a JSON array of department response objects. If no departments exist, the response is:

```json
[]
```

## Error Responses

The explicitly handled Department errors share these fields:

- `timestamp`
- `status`
- `error`

Field-validation errors also include `fieldErrors`.

Example validation response:

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

Example duplicate-code response:

```json
{
  "timestamp": "2026-09-03T13:01:47.242Z",
  "status": 409,
  "error": "A department with code 'CS' already exists"
}
```

| Status | Handled cases |
|--------|---------------|
| `400` | Field validation, malformed or missing request body, incompatible JSON values, invalid UUID |
| `404` | Department not found |
| `409` | Duplicate department code |

Other framework errors and unexpected server failures are not currently covered by this custom error-format contract.

## Project Structure

```text
src/
├── main/
│   ├── java/com/abdulrahman/university_management_system/
│   │   ├── common/exception/
│   │   ├── department/
│   │   │   ├── controller/
│   │   │   ├── dto/
│   │   │   ├── entity/
│   │   │   ├── exception/
│   │   │   ├── repository/
│   │   │   └── service/
│   │   └── UniversityManagementSystemApplication.java
│   └── resources/
│       ├── application.properties
│       └── db/migration/
│           └── V1__create_departments.sql
└── test/
    └── java/com/abdulrahman/university_management_system/
        └── department/
            ├── controller/
            ├── integration/
            └── service/
```

## Design Decisions

- Controllers handle HTTP requests and responses.
- Services contain business rules and transaction boundaries.
- Repositories provide database access.
- DTOs keep the API separate from JPA entities.
- Flyway owns schema changes; Hibernate validates the schema.
- The database unique constraint protects department codes against concurrent inserts.
- `saveAndFlush()` makes the insert occur inside the service's exception-handling block.
- Constraint inspection identifies duplicate-code failures without querying a failed transaction.
- JPA generates application-created UUIDs and timestamps. Database defaults also support inserts that omit those values outside JPA.

## Planned Features

- Program management and its relationship to Departments.
- Course management.
- Enrollment.
- Authentication and role-based authorization.

## Project Purpose

This project is developed for learning and portfolio demonstration.
