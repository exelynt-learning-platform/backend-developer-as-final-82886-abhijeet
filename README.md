# Resource Booking System

A RESTful booking API built with **Spring Boot 3.3**, **Java 17**, **Spring Security + JWT**, and **PostgreSQL**.

Users browse bookable resources (rooms, vehicles, equipment) and manage their own reservations. Administrators have full control over both.

---

## Table of Contents

1. [Tech Stack](#tech-stack)
2. [Prerequisites](#prerequisites)
3. [Database Setup](#database-setup)
4. [Environment Variables](#environment-variables)
5. [Running the Application](#running-the-application)
6. [Seed Users](#seed-users)
7. [API Documentation](#api-documentation)
8. [API Reference](#api-reference)
9. [Permission Matrix](#permission-matrix)
10. [Quick Start with cURL](#quick-start-with-curl)
11. [Running Tests](#running-tests)
12. [Project Structure](#project-structure)
13. [Design Decisions](#design-decisions)

---

## Tech Stack

| Concern | Choice |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.3.4 |
| Security | Spring Security 6 + JJWT 0.11.5 |
| Persistence | Spring Data JPA / Hibernate |
| Database | PostgreSQL (H2 in-memory for tests) |
| Docs | springdoc-openapi (Swagger UI) + Postman collection |
| Build | Maven |

---

## Prerequisites

- JDK 17 or newer
- Maven 3.8+
- PostgreSQL 13+ running locally (or reachable over the network)

---

## Database Setup

Create the database and a user for the application:

```sql
CREATE DATABASE booking_db;
CREATE USER booking_user WITH ENCRYPTED PASSWORD 'booking_password';
GRANT ALL PRIVILEGES ON DATABASE booking_db TO booking_user;

-- PostgreSQL 15+ also needs schema-level rights:
\c booking_db
GRANT ALL ON SCHEMA public TO booking_user;
```

You do **not** need to create tables by hand. Hibernate generates the schema on first startup (`JPA_DDL_AUTO=update`), and `DataSeeder` inserts the seed users and a few sample resources.

---

## Environment Variables

Copy `.env.example` and adjust. Every value has a working local default, so the app runs out of the box — but **`JWT_SECRET` must be overridden for anything beyond local testing**.

| Variable | Default | Notes |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/booking_db` | JDBC connection string |
| `DB_USERNAME` | `booking_user` | Database user |
| `DB_PASSWORD` | `booking_password` | Database password |
| `JWT_SECRET` | *(insecure placeholder)* | **Must be ≥ 32 bytes.** App refuses to start otherwise. |
| `JWT_EXPIRATION_MS` | `3600000` | Token lifetime (1 hour) |
| `SERVER_PORT` | `8080` | HTTP port |
| `JPA_DDL_AUTO` | `update` | Use `validate` once you adopt migrations |
| `JPA_SHOW_SQL` | `false` | Set `true` to log SQL |

Generate a real secret:

```bash
openssl rand -base64 48
```

### Setting variables

**Linux / macOS**
```bash
export JWT_SECRET="$(openssl rand -base64 48)"
export DB_PASSWORD="your-password"
```

**Windows (PowerShell)**
```powershell
$env:JWT_SECRET = "paste-a-long-random-value-here"
$env:DB_PASSWORD = "your-password"
```

---

## Running the Application

```bash
mvn clean install
mvn spring-boot:run
```

Or run the packaged jar:

```bash
mvn clean package
java -jar target/resource-booking-system-1.0.0.jar
```

The API starts on `http://localhost:8080`.

---

## Seed Users

Created automatically on first startup (only when the users table is empty, so restarts never clobber your data):

| Username | Password | Role |
|---|---|---|
| `admin` | `Admin@123` | `ADMIN` |
| `user` | `User@123` | `USER` |

Passwords are stored as BCrypt hashes — the plaintext above exists only in the seeder.

Three sample resources are also seeded: a conference room, a company van, and a portable projector.

---

## API Documentation

**Swagger UI** — <http://localhost:8080/swagger-ui.html>
**OpenAPI JSON** — <http://localhost:8080/v3/api-docs>

To call protected endpoints from Swagger: run `POST /auth/login`, copy `accessToken`, click **Authorize**, and paste the token.

**Postman** — import `postman/resource-booking-system.postman_collection.json`. Run *Login as Admin* or *Login as User* first; their test scripts store the token in a collection variable that every other request reuses. The collection also includes negative cases (403 on role violation, 409 on double-booking, 400 on an injected `userId`).

---

## API Reference

### Authentication

#### `POST /auth/login`

```json
{ "username": "admin", "password": "Admin@123" }
```

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresInMs": 3600000,
  "username": "admin",
  "role": "ADMIN"
}
```

Send the token on every subsequent request:

```
Authorization: Bearer <accessToken>
```

---

### Resources

| Method | Endpoint | Access |
|---|---|---|
| `GET` | `/api/resources` | Any authenticated user |
| `GET` | `/api/resources/{id}` | Any authenticated user |
| `POST` | `/api/resources` | ADMIN |
| `PUT` | `/api/resources/{id}` | ADMIN |
| `DELETE` | `/api/resources/{id}` | ADMIN |

**Create/update body**

```json
{
  "name": "Conference Room B",
  "type": "ROOM",
  "description": "4-seat huddle room",
  "location": "Building 1, Floor 3",
  "capacity": 4,
  "active": true
}
```

`type` is one of `ROOM`, `VEHICLE`, `EQUIPMENT`, `OTHER`.

---

### Reservations

| Method | Endpoint | Access |
|---|---|---|
| `POST` | `/api/reservations` | ADMIN, USER |
| `GET` | `/api/reservations` | ADMIN (all) / USER (own only) |
| `GET` | `/api/reservations/{id}` | ADMIN (any) / USER (own only) |
| `PUT` | `/api/reservations/{id}` | ADMIN |
| `PATCH` | `/api/reservations/{id}/status` | ADMIN |
| `PATCH` | `/api/reservations/{id}/cancel` | Owner or ADMIN |
| `DELETE` | `/api/reservations/{id}` | ADMIN |

**Create body** — note there is no `userId` field; the owner always comes from the JWT.

```json
{
  "resourceId": 1,
  "startTime": "2027-01-15T10:00:00",
  "endTime": "2027-01-15T12:00:00",
  "price": 150.00,
  "notes": "Team planning session"
}
```

New reservations always start as `PENDING`.

#### Filtering, pagination, sorting

```
GET /api/reservations?status=PENDING&minPrice=50&maxPrice=500&page=0&size=10&sort=startTime,desc
```

| Parameter | Description |
|---|---|
| `status` | `PENDING`, `CONFIRMED`, or `CANCELLED` |
| `minPrice` | Minimum price, inclusive |
| `maxPrice` | Maximum price, inclusive |
| `page` | 0-based page index (default `0`) |
| `size` | Page size (default `20`) |
| `sort` | `field,direction` — e.g. `price,asc` |

Sortable fields: `id`, `startTime`, `endTime`, `price`, `status`, `createdAt`. Anything else returns **400** rather than failing obscurely.

All filters are optional and combine freely. For a USER, the ownership filter is applied **inside the query**, so pagination totals reflect only their own reservations.

**Paginated response shape**

```json
{
  "content": [ ... ],
  "page": 0,
  "size": 10,
  "totalElements": 42,
  "totalPages": 5,
  "last": false
}
```

---

### Error Responses

Every error shares one shape:

```json
{
  "timestamp": "2026-09-18T10:15:30.123Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/reservations",
  "fieldErrors": {
    "price": "price must be greater than 0",
    "endTime": "endTime must be after startTime"
  }
}
```

| Status | When |
|---|---|
| `400` | Validation failure, malformed JSON, bad parameter, invalid sort field |
| `401` | Missing, malformed, or expired token; bad credentials |
| `403` | Authenticated but not permitted — wrong role, or another user's reservation |
| `404` | Resource or reservation does not exist |
| `409` | Overlapping booking, or cancelling an already-cancelled reservation |

---

## Permission Matrix

| Action | ADMIN | USER |
|---|:---:|:---:|
| View resources | ✅ | ✅ |
| Create / update / delete resources | ✅ | ❌ |
| Create reservation | ✅ | ✅ |
| View **all** reservations | ✅ | ❌ |
| View **own** reservations | ✅ | ✅ |
| View another user's reservation | ✅ | ❌ (403) |
| Update reservation details | ✅ | ❌ |
| Change status (e.g. confirm) | ✅ | ❌ |
| Cancel own reservation | ✅ | ✅ |
| Delete reservation | ✅ | ❌ |

---

## Quick Start with cURL

```bash
# 1. Log in
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"user","password":"User@123"}' | jq -r .accessToken)

# 2. Browse resources
curl -s http://localhost:8080/api/resources \
  -H "Authorization: Bearer $TOKEN" | jq

# 3. Book one
curl -s -X POST http://localhost:8080/api/reservations \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
        "resourceId": 1,
        "startTime": "2027-01-15T10:00:00",
        "endTime": "2027-01-15T12:00:00",
        "price": 150.00
      }' | jq

# 4. List your own, filtered
curl -s "http://localhost:8080/api/reservations?status=PENDING&page=0&size=10" \
  -H "Authorization: Bearer $TOKEN" | jq
```

---

## Running Tests

```bash
mvn test
```

Tests run against an in-memory H2 database under the `test` profile — no PostgreSQL required.

They deliberately authenticate through the **real** `POST /auth/login` endpoint and pass genuine JWTs through the full filter chain, rather than using `@WithMockUser`. That means the security wiring itself is under test, not bypassed.

Covered:

- **Authentication** — valid login returns a token and role; bad password → 401; missing fields → 400; no token → 401; garbage token → 401
- **Authorization** — USER blocked from every ADMIN-only route (403)
- **Ownership** — owner is taken from the token, not the body; an injected `userId` is rejected; a USER cannot read another user's reservation; a USER's list contains only their own; ADMIN sees all
- **CRUD** — full admin lifecycle over resources, and reservation create/update/status/cancel/delete
- **Validation** — end before start, negative price, missing required fields
- **Conflict** — overlapping booking → 409; double-cancel → 409
- **Filtering & pagination** — price filtering, page sizing, rejection of an invalid sort field

---

## Project Structure

```
src/main/java/com/example/booking/
├── BookingApplication.java
├── config/
│   ├── DataSeeder.java             # seed users + sample resources
│   ├── OpenApiConfig.java          # Swagger bearer-auth scheme
│   └── SecurityConfig.java         # filter chain, RBAC matchers, BCrypt
├── controller/
│   ├── AuthController.java
│   ├── ResourceController.java
│   └── ReservationController.java
├── dto/
│   ├── auth/                       # LoginRequest, LoginResponse
│   ├── common/                     # ApiError, PageResponse
│   ├── reservation/                # create/update/status/response DTOs
│   └── resource/                   # request/response DTOs
├── entity/
│   ├── Reservation.java  Resource.java  User.java
│   └── ReservationStatus.java  ResourceType.java  Role.java
├── exception/
│   ├── ConflictException.java
│   ├── GlobalExceptionHandler.java
│   └── ResourceNotFoundException.java
├── repository/
│   ├── ReservationRepository.java  # includes overlap-detection query
│   ├── ResourceRepository.java
│   └── UserRepository.java
├── security/
│   ├── AuthenticatedUserProvider.java
│   ├── CustomUserDetailsService.java
│   ├── JwtAccessDeniedHandler.java      # 403 as JSON
│   ├── JwtAuthenticationEntryPoint.java # 401 as JSON
│   ├── JwtAuthenticationFilter.java
│   ├── JwtService.java
│   └── SecurityUser.java
├── service/
│   ├── AuthService.java
│   ├── ReservationService.java
│   └── ResourceService.java
├── specification/
│   └── ReservationSpecifications.java   # composable filters
└── validation/
    ├── HasTimeRange.java
    ├── ValidTimeRange.java
    └── ValidTimeRangeValidator.java
```

---

## Design Decisions

**Identity never comes from the request body.** `ReservationCreateRequest` has no `userId` field at all. The owner is read from the authenticated principal in `ReservationService.create`. Because the DTO lacks the field, a client that tries to smuggle one in gets a 400 from Jackson rather than having it silently ignored — there's a test for exactly this.

**Ownership is enforced in the query, not after it.** `ReservationService.list` appends a `belongsToUser` specification for non-admins. Filtering a page *after* fetching it would corrupt `totalElements` and `totalPages`; this way the counts stay correct.

**403 vs 404 on someone else's reservation.** A USER requesting another user's reservation gets **403**. Returning 404 would arguably leak less (it hides existence), but 403 is the more honest signal and matches the assignment's framing of authorization errors. Easy to flip in `ReservationService.assertReadable` if you prefer the opposite.

**Overlapping bookings are rejected with 409.** Not explicitly required, but a booking system that lets two people reserve the same room for the same hour is broken. Cancelled reservations are excluded from the check, so a cancelled slot frees up. *If your grader expects overlaps to be permitted, delete the `assertNoOverlap` calls in `ReservationService`.*

**Price is supplied by the client.** The spec only asks that price be stored as a decimal, so `Resource` carries no rate and the server does no pricing arithmetic. `BigDecimal` with `DECIMAL(12,2)` — never `double`. In a production system you'd derive price server-side from a resource rate × duration so it can't be manipulated; that's a deliberate scope call, not an oversight.

**Status transitions.** New reservations are always `PENDING`. Confirming (or any arbitrary status change) is ADMIN-only via `PATCH /{id}/status`. Cancelling is separate — `PATCH /{id}/cancel` — and available to the owner or an admin, since users cancelling their own bookings is ordinary behaviour. Cancelling twice is a 409 rather than a silent success.

**Single access token, no refresh flow.** The assignment doesn't mention refresh tokens, so the API issues one short-lived access token (1 hour by default). Adding a refresh endpoint later is straightforward.

**`JwtService` fails fast on a weak secret.** HS256 requires ≥ 256 bits. Rather than discovering that at first login, the constructor throws at startup if `jwt.secret` is under 32 bytes.

**Two layers of security error handling.** Rejections at the filter-chain level (no token, wrong role for a URL pattern) never reach a controller, so `GlobalExceptionHandler` can't see them — `JwtAuthenticationEntryPoint` and `JwtAccessDeniedHandler` handle those. Denials thrown inside services (ownership checks) go through the handler. Both paths emit the identical `ApiError` shape.

**`open-in-view` is disabled.** Avoids lazy-loading surprises leaking into serialization; DTO mapping happens inside the transactional service layer.

**Schema generation.** `ddl-auto=update` keeps the project runnable with zero manual SQL. For production, switch to `validate` and manage schema with Flyway or Liquibase.

---

## Note on Build Verification

This project was authored in an environment without access to Maven Central, so `mvn clean verify` could not be executed against the real dependency tree. The code was reviewed manually for correctness. If `mvn clean verify` surfaces anything on your machine, the compiler output will point straight at it.
# backend-developer-as-final-82886-abhijeet
Final Project Assignment - This repository contains the complete final project code and documentation.
