-- Default look of the left menu: FULL (icons + names) or ICONS (collapsed, icons only). Users can still toggle it.
ALTER TABLE clinic_settings ADD COLUMN sidebar_mode VARCHAR(10) NOT NULL DEFAULT 'FULL' CHECK (sidebar_mode IN ('FULL', 'ICONS'));
