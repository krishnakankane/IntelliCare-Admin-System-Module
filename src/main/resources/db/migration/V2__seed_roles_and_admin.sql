-- =============================================
-- V2__seed_roles_and_admin.sql
-- IntelliCare — Seed default roles + admin user
-- =============================================

-- =============================================
-- ROLES
-- =============================================
INSERT INTO roles (name, description) VALUES
    ('ROLE_ADMIN',    'System administrator with full access'),
    ('ROLE_PATIENT',  'Patient with access to their own health data'),
    ('ROLE_DOCTOR',   'Medical professional with clinical access'),
    ('ROLE_HOSPITAL', 'Hospital administrator with institutional access');

-- =============================================
-- DEFAULT ADMIN USER
-- Password: Admin@12345  (BCrypt encoded)
-- MUST be changed immediately after first login
-- =============================================
INSERT INTO users (email, password_hash, first_name, last_name, is_active, is_verified)
VALUES ('admin@intellicare.com',
        '$2a$12$8eG7nzZKqBrPVHO7W3FT4ePmExZ5Qq3bM.jE1Y9RO4X0C6RJL0DGe',
        'System', 'Administrator', TRUE, TRUE);

-- Default password: Admin@12345  — CHANGE IMMEDIATELY after first login

-- Assign ROLE_ADMIN to default admin
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.email = 'admin@intellicare.com'
  AND r.name = 'ROLE_ADMIN';

-- =============================================
-- NOTIFICATION TEMPLATES
-- =============================================
INSERT INTO notification_templates (name, channel, subject, body, variables, is_active) VALUES
    ('WELCOME_EMAIL', 'EMAIL',
     'Welcome to IntelliCare!',
     'Hello {{firstName}},\n\nWelcome to IntelliCare. Your account has been created successfully.\n\nPlease log in at {{loginUrl}} to get started.\n\nRegards,\nIntelliCare Team',
     'firstName,loginUrl', TRUE),

    ('PASSWORD_RESET', 'EMAIL',
     'Reset Your IntelliCare Password',
     'Hello {{firstName}},\n\nClick the link below to reset your password:\n{{resetLink}}\n\nThis link expires in {{expiryMinutes}} minutes.\n\nIf you did not request this, please ignore this email.\n\nRegards,\nIntelliCare Team',
     'firstName,resetLink,expiryMinutes', TRUE),

    ('APPOINTMENT_REMINDER', 'EMAIL',
     'Appointment Reminder — {{doctorName}} on {{appointmentDate}}',
     'Hello {{patientName}},\n\nThis is a reminder that you have an appointment with Dr. {{doctorName}} on {{appointmentDate}} at {{appointmentTime}}.\n\nLocation: {{location}}\n\nRegards,\nIntelliCare Team',
     'patientName,doctorName,appointmentDate,appointmentTime,location', TRUE),

    ('APPOINTMENT_SMS', 'SMS',
     NULL,
     'IntelliCare: Reminder - Appointment with Dr. {{doctorName}} on {{appointmentDate}} at {{appointmentTime}}. Reply STOP to unsubscribe.',
     'doctorName,appointmentDate,appointmentTime', TRUE),

    ('IN_APP_ALERT', 'IN_APP',
     '{{title}}',
     '{{message}}',
     'title,message', TRUE);
