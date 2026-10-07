# Fitness Studio Booking System - CMPE 172

A layered Spring Boot application using Thymeleaf pages and REST endpoints,
with raw JDBC (no ORM) over PostgreSQL.

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
3. That's it - `schema.sql` and `seed.sql` in `src/main/resources` run
   automatically every time the app starts (`spring.sql.init.mode=always`).
   **Warning:** `schema.sql` currently drops and recreates the tables at startup,
   so restarting the app resets the database to the sample data.

## Run
```bash
mvn spring-boot:run
```
The app starts on `http://localhost:8080`.

## Endpoints
- `GET /` - home page
- `GET /login` - session login page
- `GET /slots` - Thymeleaf page to browse, filter, and paginate open sessions
- `GET /api/slots` - JSON list of open sessions with the same filters and pages
- `POST /customer/bookings` - customer-only booking form submission
- `GET /customer/bookings/{id}/confirmation` - owner-only booking confirmation
- `GET /customer/appointments` - customer's upcoming bookings and history
- `GET /customer/dashboard` - customer-only example page
- `GET /provider/dashboard` - provider-only example page

## Login and roles
- Login uses email and password; account records are loaded from PostgreSQL by
  `DatabaseUserDetailsService`.
- Customer records have the `CUSTOMER` role in `users`; provider records have
  the `PROVIDER` role in `providers`. Spring Security stores the authenticated
  context in the server-side HTTP session.
- Passwords are checked with BCrypt. Seed accounts share the sample password
  `password`:
  - Customers: `alex.chen@example.com`, `jamie.rivera@example.com`
  - Providers: `sarah.kim@fitstudio.com`, `mike.torres@fitstudio.com`,
    `priya.patel@fitstudio.com`
- Sign out uses a POST form and invalidates the session. CSRF protection is
  enabled by default.

## Booking and concurrency
- Booking is a customer-only POST. The service runs at `READ_COMMITTED` and
  locks the selected availability row with PostgreSQL `SELECT ... FOR UPDATE`.
- Requests for the same slot therefore wait for the first transaction to
  commit; the later request sees that the slot is booked and gets HTTP 409.
- The unique constraint on `appointments.slot_id` remains the database-level
  backstop. Booking the slot and inserting its `BOOKED` appointment happen in
  one transaction, so a failed insert also rolls back the slot update.
- The confirmation lookup checks the signed-in customer's email as well as the
  appointment ID, so another customer cannot view that confirmation.
- The appointment list is likewise queried by the signed-in customer's email.
  Past `BOOKED` appointments are changed to `COMPLETED` when the customer views
  their appointment list; cancelled/completed items appear in history.

## Architecture
- **Controller** (`controller/`) — receives HTTP requests via Spring's
  `DispatcherServlet` (Front Controller pattern) and returns pages or JSON.
- **Service** (`service/`) — business logic layer.
- **Repository** (`repository/`) — talks to PostgreSQL directly via
  `JdbcTemplate` (plain JDBC, no Hibernate/JPA).
- **DTO** (`dto/`) - flat objects returned to the client as JSON.

## Double-booking guard
- `availability_slots` has a `UNIQUE (provider_id, slot_date, start_time)`
  constraint so a provider can't create duplicate/overlapping slots.
- `appointments.slot_id` is `UNIQUE`, so the database itself guarantees a
  slot can never be booked by more than one appointment, even under
  concurrent requests.
