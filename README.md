# AuthTrack - User Authentication and Access Control System

A Spring Boot REST API for user authentication and role based authorization using JWT and Spring Security.

## Tech Stack

- Java 17
- Spring Boot 3.2
- Spring Security 6
- JWT (jjwt 0.11.5)
- MySQL 8
- JPA / Hibernate
- JUnit 5 and Mockito
- SLF4J logging
- Maven

## Setup

### 1. Create the database

```
CREATE DATABASE authtrack_db;
```

### 2. Set environment variables

Secrets are read from the environment and are never committed.

| Variable | Required | Description |
| -------- | -------- | ----------- |
| JWT_SECRET | Yes | Signing key for JWTs. Generate one with `python3 -c "import secrets; print(secrets.token_hex(32))"` |
| DB_USERNAME | No | MySQL username (default: root) |
| DB_PASSWORD | No | MySQL password (default: empty) |
| BOOTSTRAP_ADMIN_USERNAME | No | Username of the first admin, created on startup |
| BOOTSTRAP_ADMIN_EMAIL | No | Email of the first admin |
| BOOTSTRAP_ADMIN_PASSWORD | No | Password of the first admin (minimum 8 characters) |

The bootstrap admin is only created if all three BOOTSTRAP variables are set and that username does not already exist.

```
export JWT_SECRET=<your generated secret>
export BOOTSTRAP_ADMIN_USERNAME=admin
export BOOTSTRAP_ADMIN_EMAIL=admin@example.com
export BOOTSTRAP_ADMIN_PASSWORD=<at least 8 characters>
```

### 3. Run the application

```
mvn spring-boot:run
```

Roles are seeded automatically on startup (ROLE_USER, ROLE_MODERATOR, ROLE_ADMIN).

## Security Model

Public registration always creates a user with ROLE_USER. Any roles sent in the register request are ignored. Elevated roles are granted only by an admin through `PATCH /api/users/{id}/roles`. Admins cannot change their own roles.

## API Endpoints

### Auth (public)

| Method | URL                | Description                          |
| ------ | ------------------ | ------------------------------------ |
| POST   | /api/auth/register | Register a new user (ROLE_USER only) |
| POST   | /api/auth/login    | Login and receive JWT                |

### Users (protected)

| Method | URL                           | Role Required     | Description                  |
| ------ | ----------------------------- | ----------------- | ---------------------------- |
| GET    | /api/users                    | ADMIN             | List all users               |
| GET    | /api/users/{id}               | ADMIN, MODERATOR  | Get user by ID               |
| GET    | /api/users/profile/{username} | Any authenticated | View profile                 |
| PATCH  | /api/users/{id}/toggle-status | ADMIN             | Enable or disable user       |
| PATCH  | /api/users/{id}/roles         | ADMIN             | Replace a user's roles       |
| DELETE | /api/users/{id}               | ADMIN             | Delete a user                |

### Access Tests

| Method | URL                  | Role Required          |
| ------ | -------------------- | ---------------------- |
| GET    | /api/public/ping     | None                   |
| GET    | /api/user/dashboard  | USER, MODERATOR, ADMIN |
| GET    | /api/moderator/panel | MODERATOR, ADMIN       |
| GET    | /api/admin/panel     | ADMIN                  |

## Sample Requests

### Register

```
POST /api/auth/register
{
  "username": "john",
  "email": "john@example.com",
  "password": "secret123"
}
```

### Login

```
POST /api/auth/login
{
  "username": "john",
  "password": "secret123"
}
```

Response:

```
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

### Update a user's roles (admin only)

```
PATCH /api/users/2/roles
{
  "roles": ["moderator"]
}
```

Valid roles: `user`, `moderator`, `admin`.

## Running Tests

```
mvn test
```

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
      UserServiceTest.java
```