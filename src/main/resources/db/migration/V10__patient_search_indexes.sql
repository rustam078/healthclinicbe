-- Fast "contains" search on patient name, mobile and patient ID (LIKE '%text%'), even with many years of patients.
-- pg_trgm ships with PostgreSQL and is a trusted extension, so the database owner can enable it.
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX idx_patients_name_trgm  ON patients USING gin (lower(full_name) gin_trgm_ops);
CREATE INDEX idx_patients_phone_trgm ON patients USING gin (lower(phone) gin_trgm_ops);
CREATE INDEX idx_patients_code_trgm  ON patients USING gin (lower(patient_code) gin_trgm_ops);
