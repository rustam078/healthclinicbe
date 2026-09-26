-- Staff no longer see the Settings module by default (administrators can change this in Settings -> Permissions)
UPDATE role_permissions SET access = 'NONE' WHERE role = 'STAFF' AND module = 'SETTINGS';

-- Individual actions that can be allowed per role (administrators are always allowed)
CREATE TABLE role_actions (
    id         BIGSERIAL PRIMARY KEY,
    role       VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN', 'STAFF')),
    action     VARCHAR(40) NOT NULL,
    allowed    BOOLEAN     NOT NULL DEFAULT FALSE,
    deleted    BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP   NOT NULL DEFAULT now(),
    updated_at TIMESTAMP   NOT NULL DEFAULT now(),
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    CONSTRAINT uq_role_action UNIQUE (role, action)
);

INSERT INTO role_actions (role, action, allowed) VALUES
    ('ADMIN', 'APPOINTMENT_COMPLETE', TRUE),
    ('ADMIN', 'APPOINTMENT_APPROVE_DELETE', TRUE),
    ('ADMIN', 'IPD_DISCHARGE', TRUE),
    ('STAFF', 'APPOINTMENT_COMPLETE', FALSE),
    ('STAFF', 'APPOINTMENT_APPROVE_DELETE', FALSE),
    ('STAFF', 'IPD_DISCHARGE', TRUE);
