-- V4: indexes for the queries the application runs most often.
--
-- Primary keys and UNIQUE constraints already have indexes. MySQL also creates an index for a
-- foreign key automatically, but only on that one column; the composite indexes below cover the
-- whole WHERE / ORDER BY of each query, and MySQL reuses them for the foreign key as well
-- (the foreign-key column is the first column of each index).

-- Double-booking check (ADR-0004), run on every new or rescheduled reservation:
--   WHERE lab_id = ? AND status IN ('PENDING','APPROVED') AND start_time < ? AND end_time > ?
CREATE INDEX idx_reservations_lab_time       ON reservations (lab_id, start_time, end_time);
CREATE INDEX idx_reservations_equipment_time ON reservations (equipment_id, start_time, end_time);

-- Lab manager's approval queue:  WHERE status = 'PENDING' ORDER BY start_time
CREATE INDEX idx_reservations_status_start   ON reservations (status, start_time);

-- "My reservations" page:  WHERE user_id = ? ORDER BY start_time DESC
CREATE INDEX idx_reservations_user_start     ON reservations (user_id, start_time);

-- Equipment list page:  ORDER BY name
CREATE INDEX idx_equipment_name              ON equipment (name);

-- A reservation's audit trail, oldest first:  WHERE reservation_id = ? ORDER BY changed_at
CREATE INDEX idx_history_reservation_time    ON reservation_status_history (reservation_id, changed_at);

-- Open maintenance tickets for one item:  WHERE equipment_id = ? AND resolved_at IS NULL
CREATE INDEX idx_maintenance_equipment_open  ON maintenance_records (equipment_id, resolved_at);
