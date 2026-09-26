-- Remove the sample footer line seeded in V2 (only if it was never changed); the clinic sets its own in Settings.
UPDATE clinic_settings
SET footer_text = NULL
WHERE footer_text = 'Thank you for visiting. Please carry this document on your next visit.';
