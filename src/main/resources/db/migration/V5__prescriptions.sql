-- Digital prescription pad: the doctor writes by hand (stylus / finger) on a tablet or computer.
-- Ink is stored as vector strokes (JSON) so it can be re-rendered, printed and edited later.

ALTER TABLE document_templates DROP CONSTRAINT document_templates_template_type_check;
ALTER TABLE document_templates ADD CONSTRAINT document_templates_template_type_check CHECK (template_type IN
    ('APPOINTMENT_SLIP', 'ADMISSION_FORM', 'DISCHARGE_SUMMARY', 'PATIENT_PROFILE', 'REPORT', 'PRESCRIPTION'));

INSERT INTO document_templates
    (template_type, title, page_width, page_height, header_height, logo_area_width, logo_area_height, padding, margin, footer_text)
VALUES ('PRESCRIPTION', 'Prescription', 794, 1123, 120, 180, 60, 28, 12,
        'Take medicines as advised. Please bring this prescription on your next visit.');

CREATE TABLE prescriptions (
    id             BIGSERIAL PRIMARY KEY,
    appointment_id BIGINT    NOT NULL UNIQUE REFERENCES appointments (id) ON DELETE CASCADE,
    patient_id     BIGINT    NOT NULL REFERENCES patients (id),
    strokes        TEXT      NOT NULL DEFAULT '[]',
    follow_up_date DATE,
    completed_at   TIMESTAMP,
    deleted        BOOLEAN   NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMP NOT NULL DEFAULT now(),
    updated_at     TIMESTAMP NOT NULL DEFAULT now(),
    created_by     VARCHAR(50),
    updated_by     VARCHAR(50)
);
CREATE INDEX idx_prescriptions_patient ON prescriptions (patient_id);
