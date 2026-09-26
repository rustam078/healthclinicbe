-- The clinic's "Website" becomes its "Domain": the app's address on the clinic network (e.g. myprabhclinic.in).
-- Set and changed in Settings → Clinic; it is also allowed for CORS.
ALTER TABLE clinic_settings RENAME COLUMN website TO domain;

-- keep only the name from an existing web address (https://www.example.in/ -> www.example.in)
UPDATE clinic_settings
SET domain = lower(split_part(regexp_replace(trim(domain), '^[a-zA-Z]+://', ''), '/', 1))
WHERE domain IS NOT NULL AND trim(domain) <> '';

UPDATE clinic_settings SET domain = 'myprabhclinic.in' WHERE domain IS NULL OR trim(domain) = '';
