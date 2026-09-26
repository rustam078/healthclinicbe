-- Walk-in queue (first come, first served), single clinic doctor, fees from settings,
-- and appointment delete requests replacing phone-booking requests.

-- Consultation fee and free follow-up validity live in clinic settings
ALTER TABLE clinic_settings
    ADD COLUMN consultation_fee        NUMERIC(10, 2) NOT NULL DEFAULT 0 CHECK (consultation_fee >= 0),
    ADD COLUMN follow_up_validity_days INT            NOT NULL DEFAULT 30 CHECK (follow_up_validity_days BETWEEN 0 AND 365);

UPDATE clinic_settings
SET consultation_fee = COALESCE((SELECT consultation_fee FROM doctors
                                 WHERE deleted = FALSE AND active = TRUE ORDER BY id LIMIT 1), 0);

-- Phone-booking requests are no longer used
DROP TABLE appointment_requests;
DROP SEQUENCE request_code_seq;

-- Only two statuses remain: Scheduled and Completed
DELETE FROM appointments WHERE status IN ('CANCELLED', 'NO_SHOW');
UPDATE appointments SET status = 'SCHEDULED' WHERE status = 'CONFIRMED';
ALTER TABLE appointments DROP CONSTRAINT appointments_status_check;
ALTER TABLE appointments ADD CONSTRAINT appointments_status_check CHECK (status IN ('SCHEDULED', 'COMPLETED'));

-- Visit type is decided automatically: consultation or free follow-up
UPDATE appointments SET type = 'CONSULTATION' WHERE type NOT IN ('CONSULTATION', 'FOLLOW_UP');
ALTER TABLE appointments DROP CONSTRAINT appointments_type_check;
ALTER TABLE appointments ADD CONSTRAINT appointments_type_check CHECK (type IN ('CONSULTATION', 'FOLLOW_UP'));

-- Daily token numbers replace time slots
DROP INDEX uq_appointment_slot;
ALTER TABLE appointments ADD COLUMN token_number INT;
UPDATE appointments a
SET token_number = numbered.rn
FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY appointment_date ORDER BY appointment_time, id) AS rn
      FROM appointments) numbered
WHERE a.id = numbered.id;
ALTER TABLE appointments ALTER COLUMN token_number SET NOT NULL;
CREATE UNIQUE INDEX uq_appointment_token ON appointments (appointment_date, token_number);
CREATE INDEX idx_appointments_queue ON appointments (appointment_date, status, token_number);

-- Delete requests: staff ask, an administrator approves; the appointment row is then removed.
-- A snapshot of the appointment is kept on the request for the record.
CREATE TABLE appointment_delete_requests (
    id               BIGSERIAL PRIMARY KEY,
    appointment_id   BIGINT REFERENCES appointments (id) ON DELETE SET NULL,
    appointment_code VARCHAR(20)  NOT NULL,
    patient_id       BIGINT REFERENCES patients (id),
    patient_name     VARCHAR(100) NOT NULL,
    patient_phone    VARCHAR(20)  NOT NULL,
    appointment_date DATE         NOT NULL,
    token_number     INT          NOT NULL,
    status           VARCHAR(10)  NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    decided_by       VARCHAR(50),
    decided_at       TIMESTAMP,
    deleted          BOOLEAN   NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMP NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP NOT NULL DEFAULT now(),
    created_by       VARCHAR(50),
    updated_by       VARCHAR(50)
);
CREATE UNIQUE INDEX uq_delete_request_pending ON appointment_delete_requests (appointment_id) WHERE status = 'PENDING';
CREATE INDEX idx_delete_requests_status ON appointment_delete_requests (status);
