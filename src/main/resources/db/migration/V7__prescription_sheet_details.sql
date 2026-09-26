-- Prescription sheet details: handwritten weight, doctor facilities & saved signature, clinic availability text
ALTER TABLE prescriptions ADD COLUMN weight_strokes TEXT NOT NULL DEFAULT '[]';

ALTER TABLE doctors
    ADD COLUMN facilities VARCHAR(255) DEFAULT 'NICU, CPAP, Emergency & Vaccination',
    ADD COLUMN signature  TEXT;

ALTER TABLE clinic_settings ADD COLUMN availability_text VARCHAR(60) DEFAULT '24×7';
