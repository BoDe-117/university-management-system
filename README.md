# University Management System

A backend REST API for managing university departments, programs, courses,
students, and enrollments. Built as a portfolio project with Java and Spring Boot.

## Current Features

- Manage departments, programs, courses, and students.
- Enroll students in courses and list or retrieve enrollments.
- Drop or complete enrollments with controlled status transitions.
- Prevent duplicate enrollments and protect referenced records from deletion.
- Authenticate users with JWT access tokens.
- Restrict API access by role and check ownership for student data.
- Browse paginated lists with sorting and supported filters.
- Explore the API with Swagger UI.
- Apply database migrations with Flyway.
- Run automated unit, controller, PostgreSQL integration, and concurrency tests.

## Tech Stack

- Java 25
- Spring Boot 4.1.1
- Spring Web MVC and Spring Security
- Spring Data JPA and Hibernate
- PostgreSQL 18
- Flyway
- JWT
- Springdoc OpenAPI and Swagger UI
- JUnit, Mockito, MockMvc, and Testcontainers
- Maven Wrapper
- GitHub Actions

## Prerequisites

- JDK 25
- Docker Desktop, or a compatible Docker environment with Linux containers
- Git

Check the Java version used by Maven.

PowerShell:

```powershell
.\mvnw.cmd -version
```

Bash:

```bash
./mvnw -version
```

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

The local development database uses:

| Setting | Value |
|---------|-------|
| Database | `ums_dev` |
| Username | `ums_user` |
| Password | `ums_pass` |
| Host port | `5432` |

These credentials are for local development only. Do not reuse them in production.

Flyway applies migrations when the application starts. Hibernate validates the
database schema against the entity mappings.

### 3. Set a JWT secret

The application requires `JWT_SECRET`: a Base64-encoded secret containing at
least 32 random bytes. Keep the real secret outside the repository and do not
share it.

For a temporary local PowerShell session:

```powershell
$jwtKeyBytes = New-Object byte[] 32
$jwtRandom = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$jwtRandom.GetBytes($jwtKeyBytes)
$jwtRandom.Dispose()
$env:JWT_SECRET = [Convert]::ToBase64String($jwtKeyBytes)
```

For a temporary Bash session with OpenSSL installed:

```bash
export JWT_SECRET="$(openssl rand -base64 32)"
```

This value lasts only for the current terminal session. Changing the secret
invalidates previously issued tokens. For a deployment, provide a securely
stored, stable secret through the deployment environment.

### 4. Create the first administrator

The `bootstrap-admin` profile creates an administrator only if one does not
already exist. It is disabled during normal startup.

Use a password of at least 12 characters and no more than 72 UTF-8 bytes.

In PowerShell, set a username and enter a password securely:

```powershell
$env:BOOTSTRAP_ADMIN_USERNAME = Read-Host "Administrator username"
$adminSecurePassword = Read-Host "Administrator password" -AsSecureString
$adminPasswordPointer = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($adminSecurePassword)

try {
    $env:BOOTSTRAP_ADMIN_PASSWORD = [System.Runtime.InteropServices.Marshal]::PtrToStringBSTR($adminPasswordPointer)
}
finally {
    [System.Runtime.InteropServices.Marshal]::ZeroFreeBSTR($adminPasswordPointer)
}
```

Start the application with the bootstrap profile:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=bootstrap-admin"
```

When the log confirms that the administrator was created, stop the application
with `Ctrl+C`, then remove the bootstrap credentials from the terminal session:

```powershell
Remove-Item Env:BOOTSTRAP_ADMIN_USERNAME -ErrorAction SilentlyContinue
Remove-Item Env:BOOTSTRAP_ADMIN_PASSWORD -ErrorAction SilentlyContinue
Remove-Variable adminSecurePassword, adminPasswordPointer -ErrorAction SilentlyContinue
```

For Bash, set the credentials and start with the bootstrap profile:

```bash
read -r -p "Administrator username: " BOOTSTRAP_ADMIN_USERNAME
read -r -s -p "Administrator password: " BOOTSTRAP_ADMIN_PASSWORD
echo
export BOOTSTRAP_ADMIN_USERNAME BOOTSTRAP_ADMIN_PASSWORD

./mvnw spring-boot:run -Dspring-boot.run.profiles=bootstrap-admin
```

After the administrator is created, stop the application with `Ctrl+C` and
remove the credentials from the session:

```bash
unset BOOTSTRAP_ADMIN_USERNAME BOOTSTRAP_ADMIN_PASSWORD
```

Run the bootstrap profile only for initial setup. Start the application
normally afterward, without that profile. If an administrator already exists,
the bootstrap operation skips account creation.

Environment variables are visible to the application process. Use a secure
secret-management mechanism for deployments.

### 5. Run the application normally

In the same terminal session, with `JWT_SECRET` set:

PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

Bash:

```bash
./mvnw spring-boot:run
```

The API listens on `http://localhost:8081`. Running from an IntelliJ run
configuration requires setting `JWT_SECRET` in that configuration as well.

### 6. Run tests

Start Docker Desktop, then run:

PowerShell:

```powershell
.\mvnw.cmd "-DJWT_SECRET=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=" clean test
```

Bash:

```bash
JWT_SECRET=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY= ./mvnw clean test
```

The value in these commands is a public **test-only key**. Never use it for a
real deployment. GitHub Actions also uses a separate test configuration.

### 7. Browse the API documentation

Start the application, then open:

[Swagger UI](http://localhost:8081/swagger-ui.html)

The OpenAPI document is available at:

[OpenAPI JSON](http://localhost:8081/v3/api-docs)

Use the login endpoint to obtain an access token. In Swagger UI, select
**Authorize** and enter the token to call protected endpoints.

## Authentication

### Log in

```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "username": "your-username",
  "password": "your-password"
}
```

A successful response contains an `accessToken` and the token type `Bearer`.
Access tokens expire after 15 minutes. Send the token with protected requests:

```http
Authorization: Bearer <access-token>
```

Only an administrator can create accounts through `POST /api/v1/users`.
Student accounts must be linked to an existing student record. A student
record can be linked to at most one account.

## Roles and Access

| Role | Access |
|------|--------|
| `ADMIN` | Manage academic records and user accounts, including deletion. |
| `STAFF` | View, create, and update academic records; cannot delete them or create user accounts. |
| `STUDENT` | Browse departments, programs, and courses; view their own student record and enrollments; enroll themselves and drop their own enrollment. |

Only `ADMIN` and `STAFF` can complete an enrollment. Students can list their own
enrollments. If a student supplies another student's ID when listing
enrollments, the request is denied.

Requests without valid authentication receive `401`. Authenticated users
without the necessary permission receive `403`. Other supported errors include
`400` for invalid input, `404` for a missing resource, and `409` for conflicts.

The Actuator health endpoint is currently not exposed through the application's
security rules.

## Pagination

List endpoints return paginated results.

| Parameter | Default | Description |
|-----------|---------|-------------|
| `page` | `0` | Zero-based page index. |
| `size` | `20` | Number of items per page; the application limit is 100. |
| `sort` | `id,asc` | A supported field and direction, such as `name,asc`. |

For example:

```http
GET /api/v1/departments?page=0&size=10&sort=name,asc
Authorization: Bearer <access-token>
```

The response includes `content`, `totalElements`, `totalPages`, and other page
metadata. Supported sort fields depend on the endpoint. An unsupported sort
field returns `400`.

Programs can be filtered by `departmentId`; courses and students can be
filtered by `programId`; enrollments can be filtered by `studentId`,
`courseId`, or both. Access rules still apply to filtered results.

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/v1/auth/login` | Authenticate and receive a token |
| `POST` | `/api/v1/users` | Create an account; administrator only |
| `POST` | `/api/v1/departments` | Create a department |
| `GET` | `/api/v1/departments` | List departments |
| `GET` | `/api/v1/departments/{id}` | Retrieve a department |
| `PUT` | `/api/v1/departments/{id}` | Update a department |
| `DELETE` | `/api/v1/departments/{id}` | Delete a department |
| `POST` | `/api/v1/programs` | Create a program |
| `GET` | `/api/v1/programs` | List programs; optionally filter by `departmentId` |
| `GET` | `/api/v1/programs/{id}` | Retrieve a program |
| `PUT` | `/api/v1/programs/{id}` | Update a program |
| `DELETE` | `/api/v1/programs/{id}` | Delete a program |
| `POST` | `/api/v1/courses` | Create a course |
| `GET` | `/api/v1/courses` | List courses; optionally filter by `programId` |
| `GET` | `/api/v1/courses/{id}` | Retrieve a course |
| `PUT` | `/api/v1/courses/{id}` | Update a course |
| `DELETE` | `/api/v1/courses/{id}` | Delete a course |
| `POST` | `/api/v1/students` | Create a student |
| `GET` | `/api/v1/students` | List students; optionally filter by `programId` |
| `GET` | `/api/v1/students/{id}` | Retrieve a student |
| `PUT` | `/api/v1/students/{id}` | Update a student |
| `DELETE` | `/api/v1/students/{id}` | Delete a student |
| `POST` | `/api/v1/enrollments` | Enroll a student in a course |
| `GET` | `/api/v1/enrollments` | List enrollments with optional filters |
| `GET` | `/api/v1/enrollments/{id}` | Retrieve an enrollment |
| `POST` | `/api/v1/enrollments/{id}/drop` | Drop an enrollment |
| `POST` | `/api/v1/enrollments/{id}/complete` | Complete an enrollment |

See the roles table above for access restrictions. Unspecified routes are denied.

## Testing Strategy

- **Unit tests:** business rules with mocked dependencies.
- **Controller tests:** HTTP responses and validation using MockMvc.
- **Integration tests:** PostgreSQL and Flyway using Testcontainers.
- **Security tests:** authentication, roles, ownership, and protected requests.
- **Concurrency tests:** selected database constraints and state transitions.

## Future Improvements

- Refresh tokens and logout.
- Rate limiting for the login endpoint.
- An endpoint for users to view their own account.
- Additional search and filtering.
- Audit logging for administrative actions.
- CORS configuration for a frontend.

## License

This project is for educational and portfolio purposes.
