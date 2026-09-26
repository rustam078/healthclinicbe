-- Default module access per role (admin can change STAFF/ADMIN access later from Settings)
INSERT INTO role_permissions (role, module, access) VALUES
    ('ADMIN', 'DASHBOARD',   'WRITE'),
    ('ADMIN', 'APPOINTMENT', 'WRITE'),
    ('ADMIN', 'IPD',         'WRITE'),
    ('ADMIN', 'PATIENT',     'WRITE'),
    ('ADMIN', 'SETTINGS',    'WRITE'),
    ('STAFF', 'DASHBOARD',   'READ'),
    ('STAFF', 'APPOINTMENT', 'WRITE'),
    ('STAFF', 'IPD',         'WRITE'),
    ('STAFF', 'PATIENT',     'WRITE'),
    ('STAFF', 'SETTINGS',    'READ');

-- Single clinic settings row; administrators edit it from Settings
INSERT INTO clinic_settings (clinic_name, address, phone, email, header_text, footer_text)
VALUES ('My Clinic', 'Clinic address (update in Settings)', '', '', 'Quality care, close to home',
        'Thank you for visiting. Please carry this document on your next visit.');

-- Professional default templates (dimensions in CSS pixels at 96 dpi: A4 = 794 x 1123, A5 = 559 x 794)
INSERT INTO document_templates
    (template_type, title, page_width, page_height, header_height, logo_area_width, logo_area_height, padding, margin, footer_text)
VALUES
    ('APPOINTMENT_SLIP',  'Appointment Slip',   559, 794,  110, 160, 56, 24, 16, 'Please arrive 10 minutes before your appointment time.'),
    ('ADMISSION_FORM',    'Admission Record',   794, 1123, 130, 180, 60, 32, 24, 'This is a clinic record. Amounts shown are estimates, not a bill.'),
    ('DISCHARGE_SUMMARY', 'Discharge Summary',  794, 1123, 130, 180, 60, 32, 24, 'Follow the advice above and contact the clinic in case of any concern.'),
    ('PATIENT_PROFILE',   'Patient Profile',    794, 1123, 130, 180, 60, 32, 24, 'Confidential patient information.'),
    ('REPORT',            'Clinic Report',      794, 1123, 120, 180, 60, 32, 24, 'Generated from the clinic management system.');
