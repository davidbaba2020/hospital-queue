-- ════════════════════════════════════════
-- V3: Test Patients & Queue Entries
-- ════════════════════════════════════════

-- ── Sample Patients ───────────────────────────────────────────────────────────
INSERT INTO patients (patient_number, full_name, email, phone, date_of_birth, gender, blood_type, address, emergency_contact_name, emergency_contact_phone) VALUES
('PAT-00001', 'Emeka Nwosu',       'emeka.nwosu@email.com',      '+234-801-0001', '1985-03-15', 'MALE',   'O+', '12 Lagos Street, Ikeja',    'Mrs. Nwosu',     '+234-802-0001'),
('PAT-00002', 'Adaeze Obi',        'adaeze.obi@email.com',       '+234-801-0002', '1992-07-22', 'FEMALE', 'A+', '45 Eko Avenue, VI',          'Mr. Obi',        '+234-802-0002'),
('PAT-00003', 'Tunde Bakare',      'tunde.bakare@email.com',     '+234-801-0003', '1978-11-08', 'MALE',   'B+', '8 Abuja Road, Garki',        'Mrs. Bakare',    '+234-802-0003'),
('PAT-00004', 'Chidinma Eze',      'chidinma.eze@email.com',     '+234-801-0004', '2001-05-30', 'FEMALE', 'AB+','22 Trans-Amadi, PH',         'Mr. Eze',        '+234-802-0004'),
('PAT-00005', 'Yakubu Musa',       'yakubu.musa@email.com',      '+234-801-0005', '1965-09-12', 'MALE',   'O-', '3 Ahmadu Bello Way, Kaduna', 'Mrs. Musa',      '+234-802-0005'),
('PAT-00006', 'Blessing Okafor',   'blessing.okafor@email.com',  '+234-801-0006', '1995-02-18', 'FEMALE', 'A-', '17 Enugu Road, GRA',         'Mr. Okafor',     '+234-802-0006'),
('PAT-00007', 'Segun Adeleke',     'segun.adeleke@email.com',    '+234-801-0007', '1988-12-25', 'MALE',   'B-', '6 Ilorin Street, Ibadan',    'Mrs. Adeleke',   '+234-802-0007'),
('PAT-00008', 'Fatima Abubakar',   'fatima.abu@email.com',       '+234-801-0008', '1973-06-04', 'FEMALE', 'O+', '9 Maiduguri Road, Borno',    'Mr. Abubakar',   '+234-802-0008'),
('PAT-00009', 'Chukwuemeka Igwe',  'emeka.igwe@email.com',       '+234-801-0009', '2010-08-19', 'MALE',   'A+', '31 Onitsha Expressway, Aba', 'Mrs. Igwe',      '+234-802-0009'),
('PAT-00010', 'Ngozi Chukwu',      'ngozi.chukwu@email.com',     '+234-801-0010', '1980-01-07', 'FEMALE', 'AB-','14 Bank Road, Warri',        'Mr. Chukwu',     '+234-802-0010'),
('PAT-00011', 'Abdullahi Bello',   'abdullahi.b@email.com',      '+234-801-0011', '1991-04-23', 'MALE',   'B+', '2 Sultan Road, Sokoto',      'Mrs. Bello',     '+234-802-0011'),
('PAT-00012', 'Chiamaka Nze',      'chiamaka.nze@email.com',     '+234-801-0012', '1969-10-31', 'FEMALE', 'O+', '55 Ojodu Berger, Lagos',     'Mr. Nze',        '+234-802-0012');

-- ── Today's Queue Entries (mixed statuses) ────────────────────────────────────
INSERT INTO queue_entries (ticket_number, patient_id, doctor_id, department_id, priority, status, chief_complaint, registered_by, registered_at, called_at, seen_at) VALUES
-- Emergency department
('TKT-EMR-001', 1,  1, 1, 'CRITICAL', 'IN_PROGRESS', 'Severe chest pain and shortness of breath',   9, CURRENT_TIMESTAMP - INTERVAL '90' MINUTE, CURRENT_TIMESTAMP - INTERVAL '80' MINUTE, CURRENT_TIMESTAMP - INTERVAL '75' MINUTE),
('TKT-EMR-002', 2,  1, 1, 'HIGH',     'WAITING',     'Suspected fracture after fall',               9, CURRENT_TIMESTAMP - INTERVAL '60' MINUTE, NULL, NULL),
('TKT-EMR-003', 3,  1, 1, 'NORMAL',   'WAITING',     'High fever and persistent cough',             9, CURRENT_TIMESTAMP - INTERVAL '45' MINUTE, NULL, NULL),

-- General Practice
('TKT-GEN-001', 4,  2, 2, 'NORMAL',   'COMPLETED',   'Annual wellness check-up',                    10, CURRENT_TIMESTAMP - INTERVAL '120' MINUTE, CURRENT_TIMESTAMP - INTERVAL '110' MINUTE, CURRENT_TIMESTAMP - INTERVAL '100' MINUTE),
('TKT-GEN-002', 5,  2, 2, 'NORMAL',   'IN_PROGRESS', 'Persistent headache for 3 days',              10, CURRENT_TIMESTAMP - INTERVAL '55' MINUTE, CURRENT_TIMESTAMP - INTERVAL '40' MINUTE, CURRENT_TIMESTAMP - INTERVAL '35' MINUTE),
('TKT-GEN-003', 6,  2, 2, 'HIGH',     'WAITING',     'Diabetic follow-up, uncontrolled blood sugar', 10, CURRENT_TIMESTAMP - INTERVAL '30' MINUTE, NULL, NULL),
('TKT-GEN-004', 7,  2, 2, 'NORMAL',   'WAITING',     'Skin rash, spreading over 2 days',            10, CURRENT_TIMESTAMP - INTERVAL '20' MINUTE, NULL, NULL),

-- Cardiology
('TKT-CAR-001', 8,  3, 3, 'HIGH',     'COMPLETED',   'Post-surgery cardiac monitoring',             9,  CURRENT_TIMESTAMP - INTERVAL '200' MINUTE, CURRENT_TIMESTAMP - INTERVAL '190' MINUTE, CURRENT_TIMESTAMP - INTERVAL '180' MINUTE),
('TKT-CAR-002', 9,  3, 3, 'NORMAL',   'WAITING',     'Irregular heartbeat reported by wearable',   10, CURRENT_TIMESTAMP - INTERVAL '25' MINUTE, NULL, NULL),

-- Pediatrics
('TKT-PED-001', 9,  4, 4, 'HIGH',     'IN_PROGRESS', 'Child with high fever, vomiting',             9,  CURRENT_TIMESTAMP - INTERVAL '50' MINUTE, CURRENT_TIMESTAMP - INTERVAL '30' MINUTE, CURRENT_TIMESTAMP - INTERVAL '25' MINUTE),
('TKT-PED-002', 10, 4, 4, 'NORMAL',   'WAITING',     'Vaccination schedule update',                 10, CURRENT_TIMESTAMP - INTERVAL '15' MINUTE, NULL, NULL),

-- Orthopedics
('TKT-ORT-001', 11, 5, 5, 'NORMAL',   'NO_SHOW',     'Back pain physiotherapy review',              9,  CURRENT_TIMESTAMP - INTERVAL '180' MINUTE, CURRENT_TIMESTAMP - INTERVAL '150' MINUTE, NULL),
('TKT-ORT-002', 12, 5, 5, 'HIGH',     'WAITING',     'Sports injury, knee swelling',                10, CURRENT_TIMESTAMP - INTERVAL '10' MINUTE, NULL, NULL);
