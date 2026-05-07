-- ════════════════════════════════════════
-- V2: Seed Data - Departments, Users, Doctors
-- All passwords = "Password@123" (bcrypt encoded)
-- ════════════════════════════════════════

-- ── Departments ──────────────────────────────────────────────────────────────
INSERT INTO departments (name, code, description, max_queue, active) VALUES
('Emergency',          'EMR', 'Critical and urgent care unit',             30, TRUE),
('General Practice',   'GEN', 'General outpatient consultations',          50, TRUE),
('Cardiology',         'CAR', 'Heart and cardiovascular diseases',         40, TRUE),
('Pediatrics',         'PED', 'Children health and development',           45, TRUE),
('Orthopedics',        'ORT', 'Bone, joint and muscle disorders',          35, TRUE),
('Dermatology',        'DER', 'Skin, hair and nail conditions',            40, TRUE),
('Neurology',          'NEU', 'Brain and nervous system disorders',        30, TRUE),
('Ophthalmology',      'OPH', 'Eye care and vision disorders',             35, TRUE);

-- ── Users (password = Password@123) ──────────────────────────────────────────
-- ADMIN
INSERT INTO users (username, email, password_hash, full_name, phone, role, enabled) VALUES
('admin',        'admin@hospital.com',        '$2b$12$L/pqzqTrWe8OxQNmzKLqUeTcxJMfNqJ6Zy8BUg03eSQ3xx3MW2rQ.', 'System Administrator',   '+1-555-0100', 'ADMIN',        TRUE),
('superadmin',   'superadmin@hospital.com',   '$2b$12$L/pqzqTrWe8OxQNmzKLqUeTcxJMfNqJ6Zy8BUg03eSQ3xx3MW2rQ.', 'Super Administrator',    '+1-555-0101', 'ADMIN',        TRUE);

-- DOCTORS
INSERT INTO users (username, email, password_hash, full_name, phone, role, enabled) VALUES
('dr.adewale',   'dr.adewale@hospital.com',   '$2b$12$L/pqzqTrWe8OxQNmzKLqUeTcxJMfNqJ6Zy8BUg03eSQ3xx3MW2rQ.', 'Dr. Chidi Adewale',      '+1-555-0201', 'DOCTOR',       TRUE),
('dr.okonkwo',   'dr.okonkwo@hospital.com',   '$2b$12$L/pqzqTrWe8OxQNmzKLqUeTcxJMfNqJ6Zy8BUg03eSQ3xx3MW2rQ.', 'Dr. Ngozi Okonkwo',      '+1-555-0202', 'DOCTOR',       TRUE),
('dr.hassan',    'dr.hassan@hospital.com',    '$2b$12$L/pqzqTrWe8OxQNmzKLqUeTcxJMfNqJ6Zy8BUg03eSQ3xx3MW2rQ.', 'Dr. Aminu Hassan',       '+1-555-0203', 'DOCTOR',       TRUE),
('dr.eze',       'dr.eze@hospital.com',       '$2b$12$L/pqzqTrWe8OxQNmzKLqUeTcxJMfNqJ6Zy8BUg03eSQ3xx3MW2rQ.', 'Dr. Emeka Eze',          '+1-555-0204', 'DOCTOR',       TRUE),
('dr.ibrahim',   'dr.ibrahim@hospital.com',   '$2b$12$L/pqzqTrWe8OxQNmzKLqUeTcxJMfNqJ6Zy8BUg03eSQ3xx3MW2rQ.', 'Dr. Fatima Ibrahim',     '+1-555-0205', 'DOCTOR',       TRUE),
('dr.osei',      'dr.osei@hospital.com',      '$2b$12$L/pqzqTrWe8OxQNmzKLqUeTcxJMfNqJ6Zy8BUg03eSQ3xx3MW2rQ.', 'Dr. Kwame Osei',         '+1-555-0206', 'DOCTOR',       TRUE);

-- RECEPTIONISTS
INSERT INTO users (username, email, password_hash, full_name, phone, role, enabled) VALUES
('rec.aisha',    'rec.aisha@hospital.com',    '$2b$12$L/pqzqTrWe8OxQNmzKLqUeTcxJMfNqJ6Zy8BUg03eSQ3xx3MW2rQ.', 'Aisha Mohammed',         '+1-555-0301', 'RECEPTIONIST', TRUE),
('rec.funke',    'rec.funke@hospital.com',    '$2b$12$L/pqzqTrWe8OxQNmzKLqUeTcxJMfNqJ6Zy8BUg03eSQ3xx3MW2rQ.', 'Funke Adeola',           '+1-555-0302', 'RECEPTIONIST', TRUE),
('rec.musa',     'rec.musa@hospital.com',     '$2b$12$L/pqzqTrWe8OxQNmzKLqUeTcxJMfNqJ6Zy8BUg03eSQ3xx3MW2rQ.', 'Musa Suleiman',          '+1-555-0303', 'RECEPTIONIST', TRUE);

-- ── Doctor Profiles ───────────────────────────────────────────────────────────
INSERT INTO doctors (user_id, department_id, specialization, license_number, status, consultation_duration_min) VALUES
((SELECT id FROM users WHERE username='dr.adewale'), 1, 'Emergency Medicine',        'LIC-EMR-001', 'AVAILABLE', 10),
((SELECT id FROM users WHERE username='dr.okonkwo'), 2, 'General Practice',          'LIC-GEN-001', 'AVAILABLE', 15),
((SELECT id FROM users WHERE username='dr.hassan'),  3, 'Interventional Cardiology', 'LIC-CAR-001', 'BUSY',      20),
((SELECT id FROM users WHERE username='dr.eze'),     4, 'Pediatric Medicine',        'LIC-PED-001', 'AVAILABLE', 15),
((SELECT id FROM users WHERE username='dr.ibrahim'), 5, 'Orthopedic Surgery',        'LIC-ORT-001', 'ON_BREAK',  25),
((SELECT id FROM users WHERE username='dr.osei'),    6, 'Clinical Dermatology',      'LIC-DER-001', 'AVAILABLE', 15);

-- ── Sample Audit Log Entries ──────────────────────────────────────────────────
INSERT INTO audit_logs (username, action, entity_type, entity_id, status, ip_address, performed_at) VALUES
('admin',      'USER_LOGIN',    'User', '1', 'SUCCESS', '127.0.0.1', CURRENT_TIMESTAMP - INTERVAL '2' HOUR),
('admin',      'CREATE_USER',   'User', '3', 'SUCCESS', '127.0.0.1', CURRENT_TIMESTAMP - INTERVAL '1' HOUR),
('rec.aisha',  'USER_LOGIN',    'User', '9', 'SUCCESS', '127.0.0.1', CURRENT_TIMESTAMP - INTERVAL '30' MINUTE),
('admin',      'VIEW_REPORTS',  'Report', NULL, 'SUCCESS', '127.0.0.1', CURRENT_TIMESTAMP - INTERVAL '15' MINUTE);
