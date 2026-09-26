-- Clinic Management System: core schema

CREATE SEQUENCE patient_code_seq START 1;
CREATE SEQUENCE appointment_code_seq START 1;
CREATE SEQUENCE request_code_seq START 1;
CREATE SEQUENCE ipd_code_seq START 1;

-- Users & permissions -------------------------------------------------------

CREATE TABLE app_users (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    full_name     VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL CHECK (role IN ('ADMIN', 'STAFF')),
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    deleted       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP    NOT NULL DEFAULT now(),
    created_by    VARCHAR(50),
    updated_by    VARCHAR(50)
);

CREATE TABLE role_permissions (
    id         BIGSERIAL PRIMARY KEY,
    role       VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN', 'STAFF')),
    module     VARCHAR(20) NOT NULL CHECK (module IN ('DASHBOARD', 'APPOINTMENT', 'IPD', 'PATIENT', 'SETTINGS')),
    access     VARCHAR(10) NOT NULL CHECK (access IN ('NONE', 'READ', 'WRITE')),
    deleted    BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP   NOT NULL DEFAULT now(),
    updated_at TIMESTAMP   NOT NULL DEFAULT now(),
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    CONSTRAINT uq_role_module UNIQUE (role, module)
);

-- Masters -------------------------------------------------------------------

CREATE TABLE doctors (
    id               BIGSERIAL PRIMARY KEY,
    full_name        VARCHAR(100) NOT NULL,
    specialization   VARCHAR(100),
    phone            VARCHAR(20),
    email            VARCHAR(120),
    consultation_fee NUMERIC(10, 2) NOT NULL DEFAULT 0 CHECK (consultation_fee >= 0),
    active           BOOLEAN   NOT NULL DEFAULT TRUE,
    deleted          BOOLEAN   NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMP NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP NOT NULL DEFAULT now(),
    created_by       VARCHAR(50),
    updated_by       VARCHAR(50)
);

CREATE TABLE rooms (
    id           BIGSERIAL PRIMARY KEY,
    room_number  VARCHAR(20) NOT NULL,
    room_type    VARCHAR(20) NOT NULL CHECK (room_type IN ('GENERAL', 'SEMI_PRIVATE', 'PRIVATE', 'ICU')),
    floor        VARCHAR(20),
    daily_charge NUMERIC(10, 2) NOT NULL DEFAULT 0 CHECK (daily_charge >= 0),
    active       BOOLEAN   NOT NULL DEFAULT TRUE,
    deleted      BOOLEAN   NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP NOT NULL DEFAULT now(),
    updated_at   TIMESTAMP NOT NULL DEFAULT now(),
    created_by   VARCHAR(50),
    updated_by   VARCHAR(50)
);
CREATE UNIQUE INDEX uq_room_number ON rooms (lower(room_number)) WHERE deleted = FALSE;

CREATE TABLE beds (
    id         BIGSERIAL PRIMARY KEY,
    room_id    BIGINT      NOT NULL REFERENCES rooms (id),
    bed_number VARCHAR(20) NOT NULL,
    status     VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE'
               CHECK (status IN ('AVAILABLE', 'OCCUPIED', 'RESERVED', 'MAINTENANCE')),
    deleted    BOOLEAN   NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by VARCHAR(50),
    updated_by VARCHAR(50)
);
CREATE UNIQUE INDEX uq_bed_number ON beds (room_id, lower(bed_number)) WHERE deleted = FALSE;
CREATE INDEX idx_beds_status ON beds (status);

-- Patients ------------------------------------------------------------------

CREATE TABLE patients (
    id                      BIGSERIAL PRIMARY KEY,
    patient_code            VARCHAR(20)  NOT NULL UNIQUE,
    full_name               VARCHAR(100) NOT NULL,
    phone                   VARCHAR(20)  NOT NULL,
    gender                  VARCHAR(10)  NOT NULL CHECK (gender IN ('MALE', 'FEMALE', 'OTHER')),
    date_of_birth           DATE,
    address                 VARCHAR(255),
    emergency_contact_name  VARCHAR(100),
    emergency_contact_phone VARCHAR(20),
    blood_group             VARCHAR(5),
    allergies               VARCHAR(255),
    status                  VARCHAR(10) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    deleted                 BOOLEAN   NOT NULL DEFAULT FALSE,
    created_at              TIMESTAMP NOT NULL DEFAULT now(),
    updated_at              TIMESTAMP NOT NULL DEFAULT now(),
    created_by              VARCHAR(50),
    updated_by              VARCHAR(50)
);
CREATE INDEX idx_patients_name ON patients (lower(full_name));
CREATE INDEX idx_patients_phone ON patients (phone);
CREATE INDEX idx_patients_created ON patients (created_at);

-- Appointments --------------------------------------------------------------

CREATE TABLE appointments (
    id               BIGSERIAL PRIMARY KEY,
    appointment_code VARCHAR(20) NOT NULL UNIQUE,
    patient_id       BIGINT      NOT NULL REFERENCES patients (id),
    doctor_id        BIGINT      NOT NULL REFERENCES doctors (id),
    appointment_date DATE        NOT NULL,
    appointment_time TIME        NOT NULL,
    type             VARCHAR(20) NOT NULL
                     CHECK (type IN ('CONSULTATION', 'FOLLOW_UP', 'CHECKUP', 'PROCEDURE', 'OTHER')),
    reason           VARCHAR(255),
    notes            VARCHAR(1000),
    status           VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED'
                     CHECK (status IN ('SCHEDULED', 'CONFIRMED', 'COMPLETED', 'CANCELLED', 'NO_SHOW')),
    status_reason    VARCHAR(255),
    fee              NUMERIC(10, 2) NOT NULL DEFAULT 0 CHECK (fee >= 0),
    deleted          BOOLEAN   NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMP NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP NOT NULL DEFAULT now(),
    created_by       VARCHAR(50),
    updated_by       VARCHAR(50)
);
CREATE INDEX idx_appointments_date_doctor ON appointments (appointment_date, doctor_id);
CREATE INDEX idx_appointments_patient ON appointments (patient_id);
CREATE INDEX idx_appointments_status ON appointments (status);
CREATE UNIQUE INDEX uq_appointment_slot ON appointments (doctor_id, appointment_date, appointment_time)
    WHERE deleted = FALSE AND status NOT IN ('CANCELLED', 'NO_SHOW');

CREATE TABLE appointment_requests (
    id                  BIGSERIAL PRIMARY KEY,
    request_code        VARCHAR(20)  NOT NULL UNIQUE,
    patient_id          BIGINT REFERENCES patients (id),
    requester_name      VARCHAR(100) NOT NULL,
    phone               VARCHAR(20)  NOT NULL,
    preferred_doctor_id BIGINT REFERENCES doctors (id),
    preferred_date      DATE,
    preferred_time      TIME,
    reason              VARCHAR(255),
    notes               VARCHAR(1000),
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                        CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'CONVERTED')),
    status_reason       VARCHAR(255),
    appointment_id      BIGINT REFERENCES appointments (id),
    deleted             BOOLEAN   NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMP NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP NOT NULL DEFAULT now(),
    created_by          VARCHAR(50),
    updated_by          VARCHAR(50)
);
CREATE INDEX idx_requests_status ON appointment_requests (status);
CREATE INDEX idx_requests_created ON appointment_requests (created_at);

-- IPD -----------------------------------------------------------------------

CREATE TABLE ipd_admissions (
    id                      BIGSERIAL PRIMARY KEY,
    ipd_code                VARCHAR(20) NOT NULL UNIQUE,
    patient_id              BIGINT      NOT NULL REFERENCES patients (id),
    doctor_id               BIGINT      NOT NULL REFERENCES doctors (id),
    bed_id                  BIGINT      NOT NULL REFERENCES beds (id),
    admitted_at             TIMESTAMP   NOT NULL,
    expected_discharge_date DATE,
    reason                  VARCHAR(255) NOT NULL,
    notes                   VARCHAR(1000),
    status                  VARCHAR(20) NOT NULL DEFAULT 'ADMITTED' CHECK (status IN ('ADMITTED', 'DISCHARGED')),
    discharged_at           TIMESTAMP,
    discharge_condition     VARCHAR(20)
                            CHECK (discharge_condition IN ('RECOVERED', 'IMPROVED', 'REFERRED', 'AGAINST_ADVICE', 'DECEASED')),
    discharge_notes         VARCHAR(1000),
    discharge_summary       VARCHAR(2000),
    deleted                 BOOLEAN   NOT NULL DEFAULT FALSE,
    created_at              TIMESTAMP NOT NULL DEFAULT now(),
    updated_at              TIMESTAMP NOT NULL DEFAULT now(),
    created_by              VARCHAR(50),
    updated_by              VARCHAR(50),
    CONSTRAINT chk_discharge_after_admit CHECK (discharged_at IS NULL OR discharged_at >= admitted_at)
);
CREATE INDEX idx_ipd_patient ON ipd_admissions (patient_id);
CREATE INDEX idx_ipd_status ON ipd_admissions (status);
CREATE INDEX idx_ipd_admitted ON ipd_admissions (admitted_at);
CREATE UNIQUE INDEX uq_ipd_active_bed ON ipd_admissions (bed_id) WHERE status = 'ADMITTED' AND deleted = FALSE;
CREATE UNIQUE INDEX uq_ipd_active_patient ON ipd_admissions (patient_id) WHERE status = 'ADMITTED' AND deleted = FALSE;

-- Activity timeline ---------------------------------------------------------

CREATE TABLE activity_logs (
    id           BIGSERIAL PRIMARY KEY,
    entity_type  VARCHAR(30)  NOT NULL,
    entity_id    BIGINT       NOT NULL,
    patient_id   BIGINT REFERENCES patients (id),
    action       VARCHAR(30)  NOT NULL,
    description  VARCHAR(500) NOT NULL,
    performed_by VARCHAR(50),
    performed_at TIMESTAMP    NOT NULL DEFAULT now()
);
CREATE INDEX idx_activity_entity ON activity_logs (entity_type, entity_id);
CREATE INDEX idx_activity_patient ON activity_logs (patient_id);

-- Settings & templates ------------------------------------------------------

CREATE TABLE clinic_settings (
    id              BIGSERIAL PRIMARY KEY,
    clinic_name     VARCHAR(120) NOT NULL,
    address         VARCHAR(255),
    phone           VARCHAR(30),
    email           VARCHAR(120),
    website         VARCHAR(120),
    registration_no VARCHAR(60),
    header_text     VARCHAR(255),
    footer_text     VARCHAR(255),
    currency_symbol VARCHAR(5)  NOT NULL DEFAULT '₹',
    logo_path       VARCHAR(255),
    logo_width      INT NOT NULL DEFAULT 180 CHECK (logo_width BETWEEN 20 AND 600),
    logo_height     INT NOT NULL DEFAULT 60 CHECK (logo_height BETWEEN 20 AND 300),
    logo_position   VARCHAR(10) NOT NULL DEFAULT 'LEFT' CHECK (logo_position IN ('LEFT', 'CENTER', 'RIGHT')),
    deleted         BOOLEAN   NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now(),
    created_by      VARCHAR(50),
    updated_by      VARCHAR(50)
);

CREATE TABLE document_templates (
    id               BIGSERIAL PRIMARY KEY,
    template_type    VARCHAR(30)  NOT NULL UNIQUE
                     CHECK (template_type IN ('APPOINTMENT_SLIP', 'ADMISSION_FORM', 'DISCHARGE_SUMMARY', 'PATIENT_PROFILE', 'REPORT')),
    title            VARCHAR(120) NOT NULL,
    page_width       INT NOT NULL CHECK (page_width BETWEEN 200 AND 2000),
    page_height      INT NOT NULL CHECK (page_height BETWEEN 200 AND 3000),
    header_height    INT NOT NULL CHECK (header_height BETWEEN 40 AND 400),
    logo_area_width  INT NOT NULL CHECK (logo_area_width BETWEEN 20 AND 600),
    logo_area_height INT NOT NULL CHECK (logo_area_height BETWEEN 20 AND 300),
    padding          INT NOT NULL CHECK (padding BETWEEN 0 AND 100),
    margin           INT NOT NULL CHECK (margin BETWEEN 0 AND 100),
    show_logo        BOOLEAN NOT NULL DEFAULT TRUE,
    show_footer      BOOLEAN NOT NULL DEFAULT TRUE,
    footer_text      VARCHAR(255),
    accent_color     VARCHAR(7) NOT NULL DEFAULT '#0f766e',
    deleted          BOOLEAN   NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMP NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP NOT NULL DEFAULT now(),
    created_by       VARCHAR(50),
    updated_by       VARCHAR(50)
);
