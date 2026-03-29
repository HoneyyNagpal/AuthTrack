# AuthTrack - User Authentication and Access Control System

A Spring Boot REST API for user authentication and role based authorization using JWT and Spring Security.

---

## Tech Stack

- Java 17
- Spring Boot 3.2
- Spring Security 6
- JWT (jjwt 0.11.5)
- MySQL 8
- JPA / Hibernate
- Maven

---

## Setup

### 1. Create the database

```sql
CREATE DATABASE authtrack_db;
```

### 2. Configure application.properties

Open `src/main/resources/application.properties` and update these values:

```
spring.datasource.username=your_mysql_username
spring.datasource.password=your_mysql_password
```

### 3. Run the application

```bash
mvn spring-boot:run
```

Roles are seeded automatically on startup (ROLE_USER, ROLE_MODERATOR, ROLE_ADMIN).

---

## API Endpoints

### Auth (public)

| Method | URL | Description |
|--------|-----|-------------|
| POST | /api/auth/register | Register a new user |
| POST | /api/auth/login | Login and receive JWT |

### Users (protected)

| Method | URL | Role Required | Description |
|--------|-----|---------------|-------------|
| GET | /api/users | ADMIN | List all users |
| GET | /api/users/{id} | ADMIN, MODERATOR | Get user by ID |
| GET | /api/users/profile/{username} | Any authenticated | View profile |
| PATCH | /api/users/{id}/toggle-status | ADMIN | Enable or disable user |
| DELETE | /api/users/{id} | ADMIN | Delete a user |

### Access Tests

| Method | URL | Role Required |
|--------|-----|---------------|
| GET | /api/public/ping | None |
| GET | /api/user/dashboard | USER, MODERATOR, ADMIN |
| GET | /api/moderator/panel | MODERATOR, ADMIN |
| GET | /api/admin/panel | ADMIN |

---

## Sample Requests

### Register

```json
POST /api/auth/register
{
  "username": "john",
  "email": "john@example.com",
  "password": "secret123",
  "roles": ["user"]
}
```

Roles can be: `user`, `moderator`, `admin`. If omitted, defaults to `user`.

### Login

```json
POST /api/auth/login
{
  "username": "john",
  "password": "secret123"
}
```

Response:

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "type": "Bearer",
  "id": 1,
  "username": "john",
  "email": "john@example.com",
  "roles": ["ROLE_USER"]
}
```

### Authenticated Request

Pass the token in the Authorization header:

```
Authorization: Bearer <your_token_here>
```

---

## Running Tests

```bash
mvn test
```

---

## Project Structure

```
src/
  main/
    java/com/authtrack/
      config/          - SecurityConfig, DataInitializer
      controller/      - AuthController, UserController, TestController
      dto/             - Request and response models
      entity/          - User, Role
      exception/       - Custom exceptions, GlobalExceptionHandler
      repository/      - UserRepository, RoleRepository
      security/        - JwtUtils, JwtAuthFilter, UserDetailsImpl, etc.
      service/         - AuthService, UserService
    resources/
      application.properties
  test/
    java/com/authtrack/
      AuthServiceTest.java
```
