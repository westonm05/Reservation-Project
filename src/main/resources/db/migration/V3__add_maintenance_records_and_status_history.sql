-- V3: two history tables.
--
-- maintenance_records         - problems reported on a piece of equipment and how they were fixed
-- reservation_status_history  - one row every time a reservation changes status (an audit trail:
--                               who submitted, approved, rejected or cancelled it, and when)

CREATE TABLE maintenance_records (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    equipment_id     BIGINT       NOT NULL,
    reported_by_id   BIGINT       NOT NULL,
    issue            VARCHAR(500) NOT NULL,
    reported_at      DATETIME     NOT NULL,
    resolved_at      DATETIME,
    resolution_notes VARCHAR(500),
    CONSTRAINT pk_maintenance_records PRIMARY KEY (id),
    CONSTRAINT fk_maintenance_equipment FOREIGN KEY (equipment_id) REFERENCES equipment (id),
    CONSTRAINT fk_maintenance_reporter FOREIGN KEY (reported_by_id) REFERENCES users (id),
    -- an open record has no resolved_at; a closed one can't be resolved before it was reported
    CONSTRAINT chk_maintenance_dates CHECK (resolved_at IS NULL OR resolved_at >= reported_at)
);

CREATE TABLE reservation_status_history (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    reservation_id BIGINT       NOT NULL,
    old_status     VARCHAR(20),          -- NULL on the row written when the reservation is created
    new_status     VARCHAR(20)  NOT NULL,
    changed_by_id  BIGINT       NOT NULL,
    changed_at     DATETIME     NOT NULL,
    note           VARCHAR(255),
    CONSTRAINT pk_reservation_status_history PRIMARY KEY (id),
    CONSTRAINT fk_history_reservation FOREIGN KEY (reservation_id) REFERENCES reservations (id),
    CONSTRAINT fk_history_changed_by FOREIGN KEY (changed_by_id) REFERENCES users (id),
    CONSTRAINT chk_history_old_status CHECK (
        old_status IS NULL OR old_status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED')),
    CONSTRAINT chk_history_new_status CHECK (new_status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED')),
    CONSTRAINT chk_history_changes CHECK (old_status IS NULL OR old_status <> new_status)
);
