-- LabLink database schema (DDL)
-- Runs on MySQL 8 and on H2 (MODE=MySQL) for development.
-- Normalized to Third Normal Form: see docs/database-design.md.

CREATE TABLE IF NOT EXISTS users (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    email         VARCHAR(255) NOT NULL,
    full_name     VARCHAR(100) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    created_at    DATETIME     NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT chk_users_role CHECK (role IN ('STUDENT', 'FACULTY', 'LAB_MANAGER'))
);

CREATE TABLE IF NOT EXISTS labs (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL,
    building    VARCHAR(100) NOT NULL,
    room_number VARCHAR(20)  NOT NULL,
    capacity    INT          NOT NULL,
    description VARCHAR(500),
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_labs PRIMARY KEY (id),
    CONSTRAINT uq_labs_name UNIQUE (name),
    CONSTRAINT uq_labs_location UNIQUE (building, room_number),
    CONSTRAINT chk_labs_capacity CHECK (capacity > 0)
);

CREATE TABLE IF NOT EXISTS equipment (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    name          VARCHAR(100) NOT NULL,
    serial_number VARCHAR(50)  NOT NULL,
    description   VARCHAR(500),
    status        VARCHAR(20)  NOT NULL,
    lab_id        BIGINT,
    CONSTRAINT pk_equipment PRIMARY KEY (id),
    CONSTRAINT uq_equipment_serial UNIQUE (serial_number),
    CONSTRAINT fk_equipment_lab FOREIGN KEY (lab_id) REFERENCES labs (id),
    CONSTRAINT chk_equipment_status CHECK (status IN ('AVAILABLE', 'MAINTENANCE', 'RETIRED'))
);

CREATE TABLE IF NOT EXISTS reservations (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    user_id        BIGINT       NOT NULL,
    lab_id         BIGINT,
    equipment_id   BIGINT,
    start_time     DATETIME     NOT NULL,
    end_time       DATETIME     NOT NULL,
    purpose        VARCHAR(255) NOT NULL,
    status         VARCHAR(20)  NOT NULL,
    created_at     DATETIME     NOT NULL,
    reviewed_by_id BIGINT,
    reviewed_at    DATETIME,
    CONSTRAINT pk_reservations PRIMARY KEY (id),
    CONSTRAINT fk_reservations_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_reservations_lab FOREIGN KEY (lab_id) REFERENCES labs (id),
    CONSTRAINT fk_reservations_equipment FOREIGN KEY (equipment_id) REFERENCES equipment (id),
    CONSTRAINT fk_reservations_reviewer FOREIGN KEY (reviewed_by_id) REFERENCES users (id),
    CONSTRAINT chk_reservations_times CHECK (end_time > start_time),
    CONSTRAINT chk_reservations_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED')),
    -- a reservation is for exactly one lab OR one piece of equipment
    CONSTRAINT chk_reservations_target CHECK (
        (lab_id IS NOT NULL AND equipment_id IS NULL) OR
        (lab_id IS NULL AND equipment_id IS NOT NULL))
);

-- Indexes: MySQL (InnoDB) and H2 automatically index every foreign-key column, which covers
-- the double-booking lookup (lab_id / equipment_id) and the "my reservations" page (user_id).
