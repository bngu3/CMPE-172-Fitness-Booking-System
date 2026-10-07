DROP TABLE IF EXISTS appointments CASCADE;
DROP TABLE IF EXISTS availability_slots CASCADE;
DROP TABLE IF EXISTS services CASCADE;
DROP TABLE IF EXISTS providers CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- ---------------------------------------------------------------------
-- users: customers who book sessions
-- ---------------------------------------------------------------------
CREATE TABLE users (
    user_id     SERIAL PRIMARY KEY,
    full_name   VARCHAR(100) NOT NULL,
    email       VARCHAR(150) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    role        VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER' CHECK (role = 'CUSTOMER'),
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ---------------------------------------------------------------------
-- providers: trainers/instructors
-- ---------------------------------------------------------------------
CREATE TABLE providers (
    provider_id SERIAL PRIMARY KEY,
    full_name   VARCHAR(100) NOT NULL,
    email       VARCHAR(150) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    role        VARCHAR(20) NOT NULL DEFAULT 'PROVIDER' CHECK (role = 'PROVIDER'),
    specialty   VARCHAR(100),
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ---------------------------------------------------------------------
-- services: types of sessions a provider can offer
-- (1-on-1 Training, Yoga Class, Spin Class, Nutrition Consult, etc.)
-- ---------------------------------------------------------------------
CREATE TABLE services (
    service_id       SERIAL PRIMARY KEY,
    provider_id      INTEGER NOT NULL REFERENCES providers(provider_id) ON DELETE CASCADE,
    name             VARCHAR(100) NOT NULL,
    description      TEXT,
    duration_minutes INTEGER NOT NULL CHECK (duration_minutes > 0),
    price            NUMERIC(8,2) NOT NULL CHECK (price >= 0)
);

-- ---------------------------------------------------------------------
-- availability_slots: a specific bookable time window a provider opens
-- for a given service.
-- Double booking guard here
-- A provider cannot open two identical slots (same provider, same date,
-- same start time). This protects the provider's own calendar.
-- ---------------------------------------------------------------------
CREATE TABLE availability_slots (
    slot_id     SERIAL PRIMARY KEY,
    provider_id INTEGER NOT NULL REFERENCES providers(provider_id) ON DELETE CASCADE,
    service_id  INTEGER NOT NULL REFERENCES services(service_id) ON DELETE CASCADE,
    slot_date   DATE NOT NULL,
    start_time  TIME NOT NULL,
    end_time    TIME NOT NULL,
    is_booked   BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_provider_slot UNIQUE (provider_id, slot_date, start_time),
    CONSTRAINT chk_slot_time CHECK (end_time > start_time)
);

-- ---------------------------------------------------------------------
-- appointments: a customer's booking of a specific availability_slot
--
-- The partial unique index below allows cancelled appointments to remain in
-- history while still preventing two active appointments for one slot.
-- ---------------------------------------------------------------------
CREATE TABLE appointments (
    appointment_id SERIAL PRIMARY KEY,
    slot_id        INTEGER NOT NULL REFERENCES availability_slots(slot_id) ON DELETE CASCADE,
    user_id        INTEGER NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    status         VARCHAR(20) NOT NULL DEFAULT 'BOOKED'
                   CHECK (status IN ('BOOKED', 'CANCELLED', 'COMPLETED')),
    booked_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uq_active_appointment_slot ON appointments(slot_id)
    WHERE status IN ('BOOKED', 'COMPLETED');

CREATE INDEX idx_slots_provider ON availability_slots(provider_id);
CREATE INDEX idx_slots_service  ON availability_slots(service_id);
CREATE INDEX idx_slots_date     ON availability_slots(slot_date);
CREATE INDEX idx_appt_user      ON appointments(user_id);
