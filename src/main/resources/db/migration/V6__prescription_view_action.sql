-- Viewing / printing prescriptions is a separate, role-controlled action (writing stays APPOINTMENT_COMPLETE)
INSERT INTO role_actions (role, action, allowed) VALUES
    ('ADMIN', 'PRESCRIPTION_VIEW', TRUE),
    ('STAFF', 'PRESCRIPTION_VIEW', TRUE);
