# Fitness Studio Booking System — CMPE 172 Milestone 1

A layered Spring Boot REST API (Controller → Service → Repository) using
raw JDBC (no ORM) over PostgreSQL.

## Entities
`users`, `providers`, `services`, `availability_slots`, `appointments`

## Prerequisites
- Java 17+
- Maven (or use the included wrapper if added)
- PostgreSQL running locally on port 5432

## Setup
1. Create a database named `booking_app` in PostgreSQL (via pgAdmin or `createdb`).
2. Open `src/main/resources/application.properties` and set your Postgres
   username/password to match your local install.
3. That's it — `schema.sql` and `seed.sql` in `src/main/resources` run
   automatically every time the app starts (`spring.sql.init.mode=always`).

## Run
```bash
mvn spring-boot:run
```
The app starts on `http://localhost:8080`.

## Endpoints
- `GET /` — health/home message
- `GET /slots` — returns all currently available booking slots as JSON DTOs,
  joined with provider and service info

## Architecture
- **Controller** (`controller/`) — receives HTTP requests via Spring's
  `DispatcherServlet` (Front Controller pattern, since this is a REST API
  consumed by a separate frontend/SPA rather than server-rendered pages).
- **Service** (`service/`) — business logic layer.
- **Repository** (`repository/`) — talks to PostgreSQL directly via
  `JdbcTemplate` (plain JDBC, no Hibernate/JPA).
- **DTO** (`dto/`) — flat objects returned to the client as JSON.

## Double-booking guard
- `availability_slots` has a `UNIQUE (provider_id, slot_date, start_time)`
  constraint so a provider can't create duplicate/overlapping slots.
- `appointments.slot_id` is `UNIQUE`, so the database itself guarantees a
  slot can never be booked by more than one appointment, even under
  concurrent requests.
