-- Undo V12: the clinic setting is the "Website" again (V12 already ran, so it is reversed here instead of removed).
ALTER TABLE clinic_settings RENAME COLUMN domain TO website;

-- clear the placeholder V12 filled in
UPDATE clinic_settings SET website = NULL WHERE website = 'myprabhclinic.in';
