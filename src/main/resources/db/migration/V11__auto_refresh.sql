-- How often lists refresh by themselves on every device (seconds; 0 = off). Set in Settings → Clinic.
ALTER TABLE clinic_settings
    ADD COLUMN auto_refresh_seconds INT NOT NULL DEFAULT 10 CHECK (auto_refresh_seconds BETWEEN 0 AND 600);
