-- Demo / test data for LabLink.
--
-- This is a Flyway *repeatable* migration (the R__ prefix). Flyway runs it after all the
-- versioned V__ migrations, and runs it again whenever this file changes. It lives in
-- db/seed, which is only on Flyway's search path for development and tests (H2) and for the
-- optional "seed" profile - never for plain production (mysql profile).
--
-- It starts by deleting every row, so running it again always gives the same data.
-- Foreign keys are looked up by natural keys (email, lab name, serial number...) instead of
-- hard-coded ids, so the script works no matter which ids the database hands out.
--
-- Demo logins:  manager@lablink.edu / manager123   faculty@lablink.edu / faculty123
--               student@lablink.edu / student123   every other account: password123
-- Generated deterministically; see docs/database-design.md section 6 for the row counts.

-- Children first, parents last, so no foreign key is ever left dangling.
DELETE FROM reservation_status_history;
DELETE FROM maintenance_records;
DELETE FROM reservations;
DELETE FROM equipment;
DELETE FROM labs;
DELETE FROM users;
DELETE FROM equipment_categories;
DELETE FROM departments;

-- ---------------------------------------------------------------- departments (8)
INSERT INTO departments (code, name) VALUES
    ('CS', 'Computer Science'),
    ('IT', 'Information Technology'),
    ('MATH', 'Mathematics'),
    ('BIO', 'Biology'),
    ('CHEM', 'Chemistry'),
    ('PHYS', 'Physics'),
    ('ART', 'Art and Design'),
    ('COMM', 'Mass Communication');

-- ---------------------------------------------------------------- equipment_categories (10)
INSERT INTO equipment_categories (name, description, max_loan_hours) VALUES
    ('Laptop', 'Windows and macOS loaner laptops', 72),
    ('Tablet', 'iPads and Android tablets', 72),
    ('3D Printer', 'FDM and resin printers', 8),
    ('Raspberry Pi Kit', 'Single-board computer kits with sensors', 168),
    ('Arduino Kit', 'Microcontroller starter kits', 168),
    ('Oscilloscope', 'Digital storage oscilloscopes', 24),
    ('Microscope', 'Compound and digital microscopes', 24),
    ('Camera', 'DSLR, mirrorless and video cameras', 48),
    ('Projector', 'Portable projectors', 24),
    ('VR Headset', 'Virtual reality headsets', 8);

-- ---------------------------------------------------------------- users (25)
INSERT INTO users (email, full_name, password_hash, role, created_at, department_id) VALUES
    ('manager@lablink.edu', 'Demo Lab Manager', '$2b$10$t6tsU5gpNUa.yyvC4fqx8.BDvTbfnSugby3cogoV4eIqK1L4xrZa6', 'LAB_MANAGER', '2026-08-15 09:00:00', (SELECT id FROM departments WHERE code = 'CS')),
    ('faculty@lablink.edu', 'Demo Faculty', '$2b$10$eebeK0KtibsgU82LTFui6ORf6YN0miVQrja5dqd5h1Wp9QgMwd3xC', 'FACULTY', '2026-08-15 09:17:00', (SELECT id FROM departments WHERE code = 'CS')),
    ('student@lablink.edu', 'Demo Student', '$2b$10$L/iSAXuHxv6wzbjfZegiVeA58r6xT5WbtB3zPHpCdstY631/rCucS', 'STUDENT', '2026-08-15 09:34:00', (SELECT id FROM departments WHERE code = 'CS')),
    ('rhonda.keller@lablink.edu', 'Rhonda Keller', '$2b$10$BhjjHNZrEF61mABzzOREsentDKepcktYSTGiDLDjW2yO7xeSPWcbm', 'LAB_MANAGER', '2026-08-16 09:51:00', (SELECT id FROM departments WHERE code = 'BIO')),
    ('marcus.hale@lablink.edu', 'Marcus Hale', '$2b$10$BhjjHNZrEF61mABzzOREsentDKepcktYSTGiDLDjW2yO7xeSPWcbm', 'LAB_MANAGER', '2026-08-16 10:08:00', (SELECT id FROM departments WHERE code = 'ART')),
    ('elena.vasquez@lablink.edu', 'Dr. Elena Vasquez', '$2b$10$BhjjHNZrEF61mABzzOREsentDKepcktYSTGiDLDjW2yO7xeSPWcbm', 'FACULTY', '2026-08-16 10:25:00', (SELECT id FROM departments WHERE code = 'CHEM')),
    ('thomas.reed@lablink.edu', 'Dr. Thomas Reed', '$2b$10$BhjjHNZrEF61mABzzOREsentDKepcktYSTGiDLDjW2yO7xeSPWcbm', 'FACULTY', '2026-08-17 10:42:00', (SELECT id FROM departments WHERE code = 'PHYS')),
    ('grace.okafor@lablink.edu', 'Dr. Grace Okafor', '$2b$10$BhjjHNZrEF61mABzzOREsentDKepcktYSTGiDLDjW2yO7xeSPWcbm', 'FACULTY', '2026-08-17 10:59:00', (SELECT id FROM departments WHERE code = 'BIO')),
    ('david.lin@lablink.edu', 'Prof. David Lin', '$2b$10$BhjjHNZrEF61mABzzOREsentDKepcktYSTGiDLDjW2yO7xeSPWcbm', 'FACULTY', '2026-08-17 11:16:00', (SELECT id FROM departments WHERE code = 'MATH')),
    ('sarah.whitfield@lablink.edu', 'Prof. Sarah Whitfield', '$2b$10$BhjjHNZrEF61mABzzOREsentDKepcktYSTGiDLDjW2yO7xeSPWcbm', 'FACULTY', '2026-08-18 11:33:00', (SELECT id FROM departments WHERE code = 'COMM')),
    ('aiden.brooks@lablink.edu', 'Aiden Brooks', '$2b$10$BhjjHNZrEF61mABzzOREsentDKepcktYSTGiDLDjW2yO7xeSPWcbm', 'STUDENT', '2026-08-18 11:50:00', (SELECT id FROM departments WHERE code = 'CS')),
    ('maya.patel@lablink.edu', 'Maya Patel', '$2b$10$BhjjHNZrEF61mABzzOREsentDKepcktYSTGiDLDjW2yO7xeSPWcbm', 'STUDENT', '2026-08-18 12:07:00', (SELECT id FROM departments WHERE code = 'CS')),
    ('jordan.ellis@lablink.edu', 'Jordan Ellis', '$2b$10$BhjjHNZrEF61mABzzOREsentDKepcktYSTGiDLDjW2yO7xeSPWcbm', 'STUDENT', '2026-08-19 12:24:00', (SELECT id FROM departments WHERE code = 'IT')),
    ('chloe.nguyen@lablink.edu', 'Chloe Nguyen', '$2b$10$BhjjHNZrEF61mABzzOREsentDKepcktYSTGiDLDjW2yO7xeSPWcbm', 'STUDENT', '2026-08-19 12:41:00', (SELECT id FROM departments WHERE code = 'BIO')),
    ('ethan.carter@lablink.edu', 'Ethan Carter', '$2b$10$BhjjHNZrEF61mABzzOREsentDKepcktYSTGiDLDjW2yO7xeSPWcbm', 'STUDENT', '2026-08-19 12:58:00', (SELECT id FROM departments WHERE code = 'PHYS')),
    ('olivia.reyes@lablink.edu', 'Olivia Reyes', '$2b$10$BhjjHNZrEF61mABzzOREsentDKepcktYSTGiDLDjW2yO7xeSPWcbm', 'STUDENT', '2026-08-20 13:15:00', (SELECT id FROM departments WHERE code = 'CHEM')),
    ('noah.simmons@lablink.edu', 'Noah Simmons', '$2b$10$BhjjHNZrEF61mABzzOREsentDKepcktYSTGiDLDjW2yO7xeSPWcbm', 'STUDENT', '2026-08-20 13:32:00', (SELECT id FROM departments WHERE code = 'IT')),
    ('ava.morgan@lablink.edu', 'Ava Morgan', '$2b$10$BhjjHNZrEF61mABzzOREsentDKepcktYSTGiDLDjW2yO7xeSPWcbm', 'STUDENT', '2026-08-20 13:49:00', (SELECT id FROM departments WHERE code = 'ART')),
    ('liam.foster@lablink.edu', 'Liam Foster', '$2b$10$BhjjHNZrEF61mABzzOREsentDKepcktYSTGiDLDjW2yO7xeSPWcbm', 'STUDENT', '2026-08-21 14:06:00', (SELECT id FROM departments WHERE code = 'COMM')),
    ('zoe.hamilton@lablink.edu', 'Zoe Hamilton', '$2b$10$BhjjHNZrEF61mABzzOREsentDKepcktYSTGiDLDjW2yO7xeSPWcbm', 'STUDENT', '2026-08-21 14:23:00', (SELECT id FROM departments WHERE code = 'MATH')),
    ('caleb.wright@lablink.edu', 'Caleb Wright', '$2b$10$BhjjHNZrEF61mABzzOREsentDKepcktYSTGiDLDjW2yO7xeSPWcbm', 'STUDENT', '2026-08-21 14:40:00', (SELECT id FROM departments WHERE code = 'CS')),
    ('isabella.ward@lablink.edu', 'Isabella Ward', '$2b$10$BhjjHNZrEF61mABzzOREsentDKepcktYSTGiDLDjW2yO7xeSPWcbm', 'STUDENT', '2026-08-22 14:57:00', (SELECT id FROM departments WHERE code = 'BIO')),
    ('lucas.bennett@lablink.edu', 'Lucas Bennett', '$2b$10$BhjjHNZrEF61mABzzOREsentDKepcktYSTGiDLDjW2yO7xeSPWcbm', 'STUDENT', '2026-08-22 15:14:00', (SELECT id FROM departments WHERE code = 'IT')),
    ('emma.russell@lablink.edu', 'Emma Russell', '$2b$10$BhjjHNZrEF61mABzzOREsentDKepcktYSTGiDLDjW2yO7xeSPWcbm', 'STUDENT', '2026-08-22 15:31:00', (SELECT id FROM departments WHERE code = 'ART')),
    ('owen.price@lablink.edu', 'Owen Price', '$2b$10$BhjjHNZrEF61mABzzOREsentDKepcktYSTGiDLDjW2yO7xeSPWcbm', 'STUDENT', '2026-08-23 15:48:00', NULL);

-- ---------------------------------------------------------------- labs (20)
INSERT INTO labs (name, building, room_number, capacity, description, active, department_id) VALUES
    ('Computer Science Lab', 'Science Building', '210', 30, '30 workstations with IntelliJ and Java 17', TRUE, (SELECT id FROM departments WHERE code = 'CS')),
    ('Robotics Lab', 'Science Building', '118', 12, 'Workbenches and soldering stations', TRUE, (SELECT id FROM departments WHERE code = 'CS')),
    ('Media Lab', 'Library', 'B04', 8, 'Video editing suites', TRUE, (SELECT id FROM departments WHERE code = 'COMM')),
    ('Networking Lab', 'Science Building', '215', 20, 'Cisco routers and switches', TRUE, (SELECT id FROM departments WHERE code = 'IT')),
    ('Cybersecurity Lab', 'Science Building', '217', 16, 'Isolated network for security exercises', TRUE, (SELECT id FROM departments WHERE code = 'IT')),
    ('Data Science Studio', 'Science Building', '301', 24, 'Dual-monitor stations with Python and R', TRUE, (SELECT id FROM departments WHERE code = 'MATH')),
    ('Math Tutoring Center', 'Library', '120', 18, 'Whiteboards and study tables', TRUE, (SELECT id FROM departments WHERE code = 'MATH')),
    ('Biology Teaching Lab', 'Life Sciences Hall', '102', 24, 'Microscopes and lab benches', TRUE, (SELECT id FROM departments WHERE code = 'BIO')),
    ('Genetics Research Lab', 'Life Sciences Hall', '204', 8, 'PCR machines and centrifuges', TRUE, (SELECT id FROM departments WHERE code = 'BIO')),
    ('Organic Chemistry Lab', 'Life Sciences Hall', '110', 20, 'Fume hoods at every station', TRUE, (SELECT id FROM departments WHERE code = 'CHEM')),
    ('Analytical Chemistry Lab', 'Life Sciences Hall', '112', 12, 'Spectrophotometers', TRUE, (SELECT id FROM departments WHERE code = 'CHEM')),
    ('Physics Lab A', 'Science Building', '130', 24, 'Mechanics and optics benches', TRUE, (SELECT id FROM departments WHERE code = 'PHYS')),
    ('Physics Lab B', 'Science Building', '132', 24, 'Electricity and magnetism benches', TRUE, (SELECT id FROM departments WHERE code = 'PHYS')),
    ('Electronics Workshop', 'Science Building', '120', 14, 'Power supplies, multimeters and oscilloscopes', TRUE, (SELECT id FROM departments WHERE code = 'PHYS')),
    ('Photography Studio', 'Fine Arts Center', '015', 10, 'Backdrops and studio lighting', TRUE, (SELECT id FROM departments WHERE code = 'ART')),
    ('Digital Art Lab', 'Fine Arts Center', '022', 16, 'Drawing tablets and Adobe Creative Cloud', TRUE, (SELECT id FROM departments WHERE code = 'ART')),
    ('Broadcast Studio', 'Gheens Center', '100', 12, 'Cameras, teleprompter and control room', TRUE, (SELECT id FROM departments WHERE code = 'COMM')),
    ('Podcast Booth', 'Library', 'B06', 4, 'Soundproofed recording booth', TRUE, (SELECT id FROM departments WHERE code = 'COMM')),
    ('Makerspace', 'Library', 'B10', 20, '3D printers, laser cutter and hand tools', FALSE, (SELECT id FROM departments WHERE code = 'CS')),
    ('Old Computer Lab', 'Administration Building', '005', 25, 'Retired - replaced by Data Science Studio', FALSE, NULL);

-- ---------------------------------------------------------------- equipment (30)
INSERT INTO equipment (name, serial_number, description, status, lab_id, category_id) VALUES
    ('Loaner Laptop 1', 'LAP-0001', 'Dell Latitude, Windows 11', 'AVAILABLE', (SELECT id FROM labs WHERE name = 'Computer Science Lab'), (SELECT id FROM equipment_categories WHERE name = 'Laptop')),
    ('Loaner Laptop 2', 'LAP-0002', 'Dell Latitude, Windows 11', 'AVAILABLE', (SELECT id FROM labs WHERE name = 'Computer Science Lab'), (SELECT id FROM equipment_categories WHERE name = 'Laptop')),
    ('Loaner Laptop 3', 'LAP-0003', 'MacBook Air M3', 'AVAILABLE', NULL, (SELECT id FROM equipment_categories WHERE name = 'Laptop')),
    ('Loaner Laptop 4', 'LAP-0004', 'MacBook Air M3', 'MAINTENANCE', NULL, (SELECT id FROM equipment_categories WHERE name = 'Laptop')),
    ('iPad 1', 'TAB-0001', 'iPad 10th gen with Pencil', 'AVAILABLE', (SELECT id FROM labs WHERE name = 'Digital Art Lab'), (SELECT id FROM equipment_categories WHERE name = 'Tablet')),
    ('iPad 2', 'TAB-0002', 'iPad 10th gen with Pencil', 'AVAILABLE', (SELECT id FROM labs WHERE name = 'Digital Art Lab'), (SELECT id FROM equipment_categories WHERE name = 'Tablet')),
    ('Android Tablet', 'TAB-0003', 'Galaxy Tab for app testing', 'AVAILABLE', (SELECT id FROM labs WHERE name = 'Computer Science Lab'), (SELECT id FROM equipment_categories WHERE name = 'Tablet')),
    ('3D Printer', 'PRN-0001', 'Prusa MK4', 'AVAILABLE', (SELECT id FROM labs WHERE name = 'Robotics Lab'), (SELECT id FROM equipment_categories WHERE name = '3D Printer')),
    ('3D Printer 2', 'PRN-0002', 'Bambu Lab P1S', 'AVAILABLE', (SELECT id FROM labs WHERE name = 'Robotics Lab'), (SELECT id FROM equipment_categories WHERE name = '3D Printer')),
    ('Resin Printer', 'PRN-0003', 'Formlabs Form 3', 'MAINTENANCE', (SELECT id FROM labs WHERE name = 'Makerspace'), (SELECT id FROM equipment_categories WHERE name = '3D Printer')),
    ('Raspberry Pi Kit', 'RPI-0001', 'Pi 5 with sensors', 'AVAILABLE', (SELECT id FROM labs WHERE name = 'Robotics Lab'), (SELECT id FROM equipment_categories WHERE name = 'Raspberry Pi Kit')),
    ('Raspberry Pi Kit 2', 'RPI-0002', 'Pi 5 with camera module', 'AVAILABLE', (SELECT id FROM labs WHERE name = 'Robotics Lab'), (SELECT id FROM equipment_categories WHERE name = 'Raspberry Pi Kit')),
    ('Raspberry Pi Kit 3', 'RPI-0003', 'Pi 4 with touchscreen', 'AVAILABLE', (SELECT id FROM labs WHERE name = 'Networking Lab'), (SELECT id FROM equipment_categories WHERE name = 'Raspberry Pi Kit')),
    ('Arduino Starter Kit 1', 'ARD-0001', 'Arduino Uno R4 kit', 'AVAILABLE', (SELECT id FROM labs WHERE name = 'Electronics Workshop'), (SELECT id FROM equipment_categories WHERE name = 'Arduino Kit')),
    ('Arduino Starter Kit 2', 'ARD-0002', 'Arduino Uno R4 kit', 'AVAILABLE', (SELECT id FROM labs WHERE name = 'Electronics Workshop'), (SELECT id FROM equipment_categories WHERE name = 'Arduino Kit')),
    ('Arduino Mega Kit', 'ARD-0003', 'Arduino Mega with motor shield', 'RETIRED', (SELECT id FROM labs WHERE name = 'Electronics Workshop'), (SELECT id FROM equipment_categories WHERE name = 'Arduino Kit')),
    ('Oscilloscope 1', 'OSC-0001', 'Rigol DS1054Z', 'AVAILABLE', (SELECT id FROM labs WHERE name = 'Electronics Workshop'), (SELECT id FROM equipment_categories WHERE name = 'Oscilloscope')),
    ('Oscilloscope 2', 'OSC-0002', 'Rigol DS1054Z', 'AVAILABLE', (SELECT id FROM labs WHERE name = 'Physics Lab B'), (SELECT id FROM equipment_categories WHERE name = 'Oscilloscope')),
    ('Digital Microscope', 'MIC-0001', 'Celestron digital microscope', 'AVAILABLE', (SELECT id FROM labs WHERE name = 'Biology Teaching Lab'), (SELECT id FROM equipment_categories WHERE name = 'Microscope')),
    ('Compound Microscope', 'MIC-0002', 'Olympus CX23', 'MAINTENANCE', (SELECT id FROM labs WHERE name = 'Biology Teaching Lab'), (SELECT id FROM equipment_categories WHERE name = 'Microscope')),
    ('Stereo Microscope', 'MIC-0003', 'AmScope dissecting microscope', 'AVAILABLE', (SELECT id FROM labs WHERE name = 'Genetics Research Lab'), (SELECT id FROM equipment_categories WHERE name = 'Microscope')),
    ('Canon DSLR', 'CAM-0001', 'Canon EOS 90D with 18-135mm lens', 'AVAILABLE', (SELECT id FROM labs WHERE name = 'Photography Studio'), (SELECT id FROM equipment_categories WHERE name = 'Camera')),
    ('Sony Mirrorless', 'CAM-0002', 'Sony a7 IV', 'AVAILABLE', (SELECT id FROM labs WHERE name = 'Photography Studio'), (SELECT id FROM equipment_categories WHERE name = 'Camera')),
    ('Video Camera', 'CAM-0003', 'Panasonic HC-X1500', 'AVAILABLE', (SELECT id FROM labs WHERE name = 'Broadcast Studio'), (SELECT id FROM equipment_categories WHERE name = 'Camera')),
    ('Old Camcorder', 'CAM-0004', 'Replaced by HC-X1500', 'RETIRED', (SELECT id FROM labs WHERE name = 'Broadcast Studio'), (SELECT id FROM equipment_categories WHERE name = 'Camera')),
    ('Portable Projector', 'PRJ-0001', 'Checked out from the front desk', 'AVAILABLE', NULL, (SELECT id FROM equipment_categories WHERE name = 'Projector')),
    ('Portable Projector 2', 'PRJ-0002', 'Epson short-throw', 'AVAILABLE', NULL, (SELECT id FROM equipment_categories WHERE name = 'Projector')),
    ('VR Headset 1', 'VR-0001', 'Meta Quest 3', 'AVAILABLE', (SELECT id FROM labs WHERE name = 'Data Science Studio'), (SELECT id FROM equipment_categories WHERE name = 'VR Headset')),
    ('VR Headset 2', 'VR-0002', 'Meta Quest 3', 'AVAILABLE', (SELECT id FROM labs WHERE name = 'Data Science Studio'), (SELECT id FROM equipment_categories WHERE name = 'VR Headset')),
    ('Podcast Microphone Kit', 'MIC-AUD-01', 'Two Shure MV7 mics and stands', 'AVAILABLE', (SELECT id FROM labs WHERE name = 'Podcast Booth'), NULL);

-- ---------------------------------------------------------------- reservations (40)
-- Past ones are finished (approved, rejected or cancelled); future ones include pending requests
-- waiting in the lab manager's queue. No two pending/approved bookings of the same lab or item overlap.
INSERT INTO reservations (user_id, lab_id, equipment_id, start_time, end_time, purpose, status, created_at, reviewed_by_id, reviewed_at) VALUES
    ((SELECT id FROM users WHERE email = 'faculty@lablink.edu'), NULL, (SELECT id FROM equipment WHERE serial_number = 'MIC-0001'), '2026-09-01 10:00:00', '2026-09-02 10:00:00', 'Field recording', 'CANCELLED', '2026-08-24 06:00:00', NULL, NULL),
    ((SELECT id FROM users WHERE email = 'faculty@lablink.edu'), (SELECT id FROM labs WHERE name = 'Media Lab'), NULL, '2026-09-04 09:00:00', '2026-09-04 12:00:00', 'Workshop', 'APPROVED', '2026-08-28 04:00:00', NULL, NULL),
    ((SELECT id FROM users WHERE email = 'grace.okafor@lablink.edu'), (SELECT id FROM labs WHERE name = 'Computer Science Lab'), NULL, '2026-09-08 14:00:00', '2026-09-08 15:00:00', 'Club meeting', 'APPROVED', '2026-09-02 09:00:00', NULL, NULL),
    ((SELECT id FROM users WHERE email = 'lucas.bennett@lablink.edu'), (SELECT id FROM labs WHERE name = 'Media Lab'), NULL, '2026-09-10 15:00:00', '2026-09-10 17:00:00', 'Tutoring session', 'CANCELLED', '2026-09-03 10:00:00', NULL, NULL),
    ((SELECT id FROM users WHERE email = 'student@lablink.edu'), (SELECT id FROM labs WHERE name = 'Cybersecurity Lab'), NULL, '2026-09-07 16:00:00', '2026-09-07 18:00:00', 'Workshop', 'APPROVED', '2026-09-05 16:00:00', (SELECT id FROM users WHERE email = 'manager@lablink.edu'), '2026-09-06 09:00:00'),
    ((SELECT id FROM users WHERE email = 'ava.morgan@lablink.edu'), NULL, (SELECT id FROM equipment WHERE serial_number = 'LAP-0002'), '2026-09-14 14:00:00', '2026-09-16 14:00:00', 'Printing project parts', 'APPROVED', '2026-09-09 10:00:00', (SELECT id FROM users WHERE email = 'manager@lablink.edu'), '2026-09-09 23:00:00'),
    ((SELECT id FROM users WHERE email = 'faculty@lablink.edu'), (SELECT id FROM labs WHERE name = 'Cybersecurity Lab'), NULL, '2026-09-22 13:00:00', '2026-09-22 15:00:00', 'Office hours overflow', 'APPROVED', '2026-09-18 13:00:00', NULL, NULL),
    ((SELECT id FROM users WHERE email = 'emma.russell@lablink.edu'), NULL, (SELECT id FROM equipment WHERE serial_number = 'MIC-0003'), '2026-09-25 10:00:00', '2026-09-25 14:00:00', 'Sample imaging', 'APPROVED', '2026-09-21 10:00:00', (SELECT id FROM users WHERE email = 'rhonda.keller@lablink.edu'), '2026-09-22 08:00:00'),
    ((SELECT id FROM users WHERE email = 'faculty@lablink.edu'), (SELECT id FROM labs WHERE name = 'Robotics Lab'), NULL, '2026-09-28 16:00:00', '2026-09-28 17:00:00', 'Lab practical', 'APPROVED', '2026-09-22 12:00:00', NULL, NULL),
    ((SELECT id FROM users WHERE email = 'caleb.wright@lablink.edu'), NULL, (SELECT id FROM equipment WHERE serial_number = 'MIC-0003'), '2026-10-07 15:00:00', '2026-10-07 18:00:00', 'Presentation', 'APPROVED', '2026-09-29 12:00:00', (SELECT id FROM users WHERE email = 'rhonda.keller@lablink.edu'), '2026-09-30 12:00:00'),
    ((SELECT id FROM users WHERE email = 'lucas.bennett@lablink.edu'), (SELECT id FROM labs WHERE name = 'Math Tutoring Center'), NULL, '2026-10-07 16:00:00', '2026-10-07 18:00:00', 'Capstone team meeting', 'CANCELLED', '2026-09-29 12:00:00', NULL, NULL),
    ((SELECT id FROM users WHERE email = 'liam.foster@lablink.edu'), NULL, (SELECT id FROM equipment WHERE serial_number = 'ARD-0001'), '2026-10-06 11:00:00', '2026-10-07 11:00:00', 'Field recording', 'APPROVED', '2026-09-30 07:00:00', (SELECT id FROM users WHERE email = 'marcus.hale@lablink.edu'), '2026-09-30 17:00:00'),
    ((SELECT id FROM users WHERE email = 'ava.morgan@lablink.edu'), (SELECT id FROM labs WHERE name = 'Physics Lab A'), NULL, '2026-10-12 14:00:00', '2026-10-12 16:00:00', 'Recording session', 'APPROVED', '2026-10-03 09:00:00', (SELECT id FROM users WHERE email = 'rhonda.keller@lablink.edu'), '2026-10-04 00:00:00'),
    ((SELECT id FROM users WHERE email = 'lucas.bennett@lablink.edu'), (SELECT id FROM labs WHERE name = 'Physics Lab B'), NULL, '2026-10-08 11:00:00', '2026-10-08 13:00:00', 'Photo shoot', 'REJECTED', '2026-10-05 11:00:00', (SELECT id FROM users WHERE email = 'rhonda.keller@lablink.edu'), '2026-10-05 15:00:00'),
    ((SELECT id FROM users WHERE email = 'olivia.reyes@lablink.edu'), (SELECT id FROM labs WHERE name = 'Networking Lab'), NULL, '2026-10-09 16:00:00', '2026-10-09 18:00:00', 'Lab practical', 'APPROVED', '2026-10-05 16:00:00', (SELECT id FROM users WHERE email = 'manager@lablink.edu'), '2026-10-05 20:00:00'),
    ((SELECT id FROM users WHERE email = 'student@lablink.edu'), (SELECT id FROM labs WHERE name = 'Electronics Workshop'), NULL, '2026-10-19 13:00:00', '2026-10-19 15:00:00', 'Photo shoot', 'APPROVED', '2026-10-10 09:00:00', (SELECT id FROM users WHERE email = 'manager@lablink.edu'), '2026-10-10 18:00:00'),
    ((SELECT id FROM users WHERE email = 'ava.morgan@lablink.edu'), NULL, (SELECT id FROM equipment WHERE serial_number = 'MIC-AUD-01'), '2026-10-20 08:00:00', '2026-10-22 08:00:00', 'Senior project prototype', 'REJECTED', '2026-10-12 07:00:00', (SELECT id FROM users WHERE email = 'marcus.hale@lablink.edu'), '2026-10-12 11:00:00'),
    ((SELECT id FROM users WHERE email = 'student@lablink.edu'), (SELECT id FROM labs WHERE name = 'Biology Teaching Lab'), NULL, '2026-10-15 08:00:00', '2026-10-15 10:00:00', 'Thesis work', 'APPROVED', '2026-10-13 08:00:00', (SELECT id FROM users WHERE email = 'manager@lablink.edu'), '2026-10-13 20:00:00'),
    ((SELECT id FROM users WHERE email = 'aiden.brooks@lablink.edu'), (SELECT id FROM labs WHERE name = 'Electronics Workshop'), NULL, '2026-10-26 08:00:00', '2026-10-26 09:00:00', 'Research data collection', 'REJECTED', '2026-10-19 06:00:00', (SELECT id FROM users WHERE email = 'manager@lablink.edu'), '2026-10-19 21:00:00'),
    ((SELECT id FROM users WHERE email = 'student@lablink.edu'), (SELECT id FROM labs WHERE name = 'Networking Lab'), NULL, '2026-10-27 13:00:00', '2026-10-27 14:00:00', 'Exam review session', 'APPROVED', '2026-10-21 11:00:00', (SELECT id FROM users WHERE email = 'marcus.hale@lablink.edu'), '2026-10-21 16:00:00'),
    ((SELECT id FROM users WHERE email = 'sarah.whitfield@lablink.edu'), (SELECT id FROM labs WHERE name = 'Media Lab'), NULL, '2026-11-09 16:00:00', '2026-11-09 18:00:00', 'Club meeting', 'APPROVED', '2026-11-01 14:00:00', NULL, NULL),
    ((SELECT id FROM users WHERE email = 'isabella.ward@lablink.edu'), NULL, (SELECT id FROM equipment WHERE serial_number = 'TAB-0002'), '2026-12-07 09:00:00', '2026-12-09 09:00:00', 'Field recording', 'CANCELLED', '2026-11-01 16:00:00', NULL, NULL),
    ((SELECT id FROM users WHERE email = 'jordan.ellis@lablink.edu'), NULL, (SELECT id FROM equipment WHERE serial_number = 'VR-0002'), '2026-12-09 15:00:00', '2026-12-09 18:00:00', 'Mobile app testing', 'PENDING', '2026-11-01 19:00:00', NULL, NULL),
    ((SELECT id FROM users WHERE email = 'grace.okafor@lablink.edu'), (SELECT id FROM labs WHERE name = 'Cybersecurity Lab'), NULL, '2026-12-16 09:00:00', '2026-12-16 11:00:00', 'Workshop', 'APPROVED', '2026-11-01 19:00:00', NULL, NULL),
    ((SELECT id FROM users WHERE email = 'liam.foster@lablink.edu'), (SELECT id FROM labs WHERE name = 'Broadcast Studio'), NULL, '2026-12-18 10:00:00', '2026-12-18 12:00:00', 'Project demo rehearsal', 'CANCELLED', '2026-11-01 19:00:00', NULL, NULL),
    ((SELECT id FROM users WHERE email = 'caleb.wright@lablink.edu'), (SELECT id FROM labs WHERE name = 'Podcast Booth'), NULL, '2026-12-28 13:00:00', '2026-12-28 14:00:00', 'Guest lecture', 'PENDING', '2026-11-01 20:00:00', NULL, NULL),
    ((SELECT id FROM users WHERE email = 'jordan.ellis@lablink.edu'), NULL, (SELECT id FROM equipment WHERE serial_number = 'TAB-0003'), '2026-11-25 11:00:00', '2026-11-25 13:00:00', 'Printing project parts', 'PENDING', '2026-11-01 20:00:00', NULL, NULL),
    ((SELECT id FROM users WHERE email = 'zoe.hamilton@lablink.edu'), (SELECT id FROM labs WHERE name = 'Data Science Studio'), NULL, '2026-12-01 16:00:00', '2026-12-01 17:00:00', 'Guest lecture', 'PENDING', '2026-11-01 21:00:00', NULL, NULL),
    ((SELECT id FROM users WHERE email = 'zoe.hamilton@lablink.edu'), (SELECT id FROM labs WHERE name = 'Electronics Workshop'), NULL, '2026-11-30 09:00:00', '2026-11-30 10:00:00', 'Exam review session', 'PENDING', '2026-11-01 22:00:00', NULL, NULL),
    ((SELECT id FROM users WHERE email = 'marcus.hale@lablink.edu'), (SELECT id FROM labs WHERE name = 'Networking Lab'), NULL, '2027-01-11 15:00:00', '2027-01-11 17:00:00', 'Thesis work', 'APPROVED', '2026-11-01 23:00:00', NULL, NULL),
    ((SELECT id FROM users WHERE email = 'ethan.carter@lablink.edu'), NULL, (SELECT id FROM equipment WHERE serial_number = 'RPI-0002'), '2027-01-14 15:00:00', '2027-01-14 21:00:00', 'Sample imaging', 'APPROVED', '2026-11-02 01:00:00', (SELECT id FROM users WHERE email = 'manager@lablink.edu'), '2026-11-02 16:00:00'),
    ((SELECT id FROM users WHERE email = 'sarah.whitfield@lablink.edu'), (SELECT id FROM labs WHERE name = 'Robotics Lab'), NULL, '2026-12-10 08:00:00', '2026-12-10 11:00:00', 'Exam review session', 'APPROVED', '2026-11-02 03:00:00', NULL, NULL),
    ((SELECT id FROM users WHERE email = 'isabella.ward@lablink.edu'), NULL, (SELECT id FROM equipment WHERE serial_number = 'RPI-0001'), '2026-11-30 10:00:00', '2026-11-30 18:00:00', 'Field recording', 'PENDING', '2026-11-02 04:00:00', NULL, NULL),
    ((SELECT id FROM users WHERE email = 'ava.morgan@lablink.edu'), (SELECT id FROM labs WHERE name = 'Networking Lab'), NULL, '2027-01-06 09:00:00', '2027-01-06 11:00:00', 'Research data collection', 'PENDING', '2026-11-02 07:00:00', NULL, NULL),
    ((SELECT id FROM users WHERE email = 'maya.patel@lablink.edu'), (SELECT id FROM labs WHERE name = 'Genetics Research Lab'), NULL, '2026-12-04 16:00:00', '2026-12-04 17:00:00', 'Thesis work', 'PENDING', '2026-11-02 11:00:00', NULL, NULL),
    ((SELECT id FROM users WHERE email = 'ava.morgan@lablink.edu'), NULL, (SELECT id FROM equipment WHERE serial_number = 'RPI-0002'), '2026-11-16 13:00:00', '2026-11-16 19:00:00', 'Class assignment', 'APPROVED', '2026-11-02 23:00:00', (SELECT id FROM users WHERE email = 'rhonda.keller@lablink.edu'), '2026-11-03 03:00:00'),
    ((SELECT id FROM users WHERE email = 'zoe.hamilton@lablink.edu'), NULL, (SELECT id FROM equipment WHERE serial_number = 'CAM-0001'), '2026-11-23 15:00:00', '2026-11-23 18:00:00', 'Video project', 'PENDING', '2026-11-03 05:00:00', NULL, NULL),
    ((SELECT id FROM users WHERE email = 'liam.foster@lablink.edu'), (SELECT id FROM labs WHERE name = 'Analytical Chemistry Lab'), NULL, '2026-12-02 16:00:00', '2026-12-02 19:00:00', 'Office hours overflow', 'APPROVED', '2026-11-03 09:00:00', (SELECT id FROM users WHERE email = 'manager@lablink.edu'), '2026-11-03 11:30:00'),
    ((SELECT id FROM users WHERE email = 'isabella.ward@lablink.edu'), (SELECT id FROM labs WHERE name = 'Physics Lab A'), NULL, '2026-12-14 15:00:00', '2026-12-14 17:00:00', 'Project demo rehearsal', 'PENDING', '2026-11-03 11:00:00', NULL, NULL),
    ((SELECT id FROM users WHERE email = 'noah.simmons@lablink.edu'), (SELECT id FROM labs WHERE name = 'Organic Chemistry Lab'), NULL, '2026-11-12 13:00:00', '2026-11-12 15:00:00', 'Photo shoot', 'APPROVED', '2026-11-03 11:00:00', (SELECT id FROM users WHERE email = 'marcus.hale@lablink.edu'), '2026-11-03 11:30:00');

-- ---------------------------------------------------------------- reservation_status_history (62)
-- One row per status change. A reservation is identified by its requester + start time
-- (unique in this data set, since nobody can be in two places at once).
INSERT INTO reservation_status_history (reservation_id, old_status, new_status, changed_by_id, changed_at, note) VALUES
    ((SELECT r.id FROM reservations r JOIN users u ON u.id = r.user_id WHERE u.email = 'faculty@lablink.edu' AND r.start_time = '2026-09-01 10:00:00'), NULL, 'APPROVED', (SELECT id FROM users WHERE email = 'faculty@lablink.edu'), '2026-08-24 06:00:00', 'Submitted and approved automatically'),
    ((SELECT r.id FROM reservations r JOIN users u ON u.id = r.user_id WHERE u.email = 'faculty@lablink.edu' AND r.start_time = '2026-09-01 10:00:00'), 'APPROVED', 'CANCELLED', (SELECT id FROM users WHERE email = 'faculty@lablink.edu'), '2026-08-25 00:00:00', 'Cancelled by requester'),
    ((SELECT r.id FROM reservations r JOIN users u ON u.id = r.user_id WHERE u.email = 'faculty@lablink.edu' AND r.start_time = '2026-09-04 09:00:00'), NULL, 'APPROVED', (SELECT id FROM users WHERE email = 'faculty@lablink.edu'), '2026-08-28 04:00:00', 'Submitted and approved automatically'),
    ((SELECT r.id FROM reservations r JOIN users u ON u.id = r.user_id WHERE u.email = 'grace.okafor@lablink.edu' AND r.start_time = '2026-09-08 14:00:00'), NULL, 'APPROVED', (SELECT id FROM users WHERE email = 'grace.okafor@lablink.edu'), '2026-09-02 09:00:00', 'Submitted and approved automatically'),
    ((SELECT r.id FROM reservations r JOIN users u ON u.id = r.user_id WHERE u.email = 'lucas.bennett@lablink.edu' AND r.start_time = '2026-09-10 15:00:00'), NULL, 'PENDING', (SELECT id FROM users WHERE email = 'lucas.bennett@lablink.edu'), '2026-09-03 10:00:00', 'Submitted for approval'),
    ((SELECT r.id FROM reservations r JOIN users u ON u.id = r.user_id WHERE u.email = 'lucas.bennett@lablink.edu' AND r.start_time = '2026-09-10 15:00:00'), 'PENDING', 'CANCELLED', (SELECT id FROM users WHERE email = 'lucas.bennett@lablink.edu'), '2026-09-03 18:00:00', 'Cancelled by requester'),
    ((SELECT r.id FROM reservations r JOIN users u ON u.id = r.user_id WHERE u.email = 'student@lablink.edu' AND r.start_time = '2026-09-07 16:00:00'), NULL, 'PENDING', (SELECT id FROM users WHERE email = 'student@lablink.edu'), '2026-09-05 16:00:00', 'Submitted for approval'),
    ((SELECT r.id FROM reservations r JOIN users u ON u.id = r.user_id WHERE u.email = 'student@lablink.edu' AND r.start_time = '2026-09-07 16:00:00'), 'PENDING', 'APPROVED', (SELECT id FROM users WHERE email = 'manager@lablink.edu'), '2026-09-06 09:00:00', 'Approved by lab manager'),
    ((SELECT r.id FROM reservations r JOIN users u ON u.id = r.user_id WHERE u.email = 'ava.morgan@lablink.edu' AND r.start_time = '2026-09-14 14:00:00'), NULL, 'PENDING', (SELECT id FROM users WHERE email = 'ava.morgan@lablink.edu'), '2026-09-09 10:00:00', 'Submitted for approval'),
    ((SELECT r.id FROM reservations r JOIN users u ON u.id = r.user_id WHERE u.email = 'ava.morgan@lablink.edu' AND r.start_time = '2026-09-14 14:00:00'), 'PENDING', 'APPROVED', (SELECT id FROM users WHERE email = 'manager@lablink.edu'), '2026-09-09 23:00:00', 'Approved by lab manager'),
    ((SELECT r.id FROM reservations r JOIN users u ON u.id = r.user_id WHERE u.email = 'faculty@lablink.edu' AND r.start_time = '2026-09-22 13:00:00'), NULL, 'APPROVED', (SELECT id FROM users WHERE email = 'faculty@lablink.edu'), '2026-09-18 13:00:00', 'Submitted and approved automatically'),
    ((SELECT r.id FROM reservations r JOIN users u ON u.id = r.user_id WHERE u.email = 'emma.russell@lablink.edu' AND r.start_time = '2026-09-25 10:00:00'), NULL, 'PENDING', (SELECT id FROM users WHERE email = 'emma.russell@lablink.edu'), '2026-09-21 10:00:00', 'Submitted for approval'),
    ((SELECT r.id FROM reservations r JOIN users u ON u.id = r.user_id WHERE u.email = 'emma.russell@lablink.edu' AND r.start_time = '2026-09-25 10:00:00'), 'PENDING', 'APPROVED', (SELECT id FROM users WHERE email = 'rhonda.keller@lablink.edu'), '2026-09-22 08:00:00', 'Approved by lab manager'),
    ((SELECT r.id FROM reservations r JOIN users u ON u.id = r.user_id WHERE u.email = 'faculty@lablink.edu' AND r.start_time = '2026-09-28 16:00:00'), NULL, 'APPROVED', (SELECT id FROM users WHERE email = 'faculty@lablink.edu'), '2026-09-22 12:00:00', 'Submitted and approved automatically'),
    ((SELECT r.id FROM reservations r JOIN users u ON u.id = r.user_id WHERE u.email = 'caleb.wright@lablink.edu' AND r.start_time = '2026-10-07 15:00:00'), NULL, 'PENDING', (SELECT id FROM users WHERE email = 'caleb.wright@lablink.edu'), '2026-09-29 12:00:00', 'Submitted for approval'),
    ((SELECT r.id FROM reservations r JOIN users u ON u.id = r.user_id WHERE u.email = 'lucas.bennett@lablink.edu' AND r.start_time = '2026-10-07 16:00:00'), NULL, 'PENDING', (SELECT id FROM users WHERE email = 'lucas.bennett@lablink.edu'), '2026-09-29 12:00:00', 'Submitted for approval'),
    ((SELECT r.id FROM reservations r JOIN users u ON u.id = r.user_id WHERE u.email = 'lucas.bennett@lablink.edu' AND r.start_time = '2026-10-07 16:00:00'), 'PENDING', 'CANCELLED', (SELECT id FROM users WHERE email = 'lucas.bennett@lablink.edu'), '2026-09-30 05:00:00', 'Cancelled by requester'),
    ((SELECT r.id FROM reservations r JOIN users u ON u.id = r.user_id WHERE u.email = 'liam.foster@lablink.edu' AND r.start_time = '2026-10-06 11:00:00'), NULL, 'PENDING', (SELECT id FROM users WHERE email = 'liam.foster@lablink.edu'), '2026-09-30 07:00:00', 'Submitted for approval'),
    ((SELECT r.id FROM reservations r JOIN users u ON u.id = r.user_id WHERE u.email = 'caleb.wright@lablink.edu' AND r.start_time = '2026-10-07 15:00:00'), 'PENDING', 'APPROVED', (SELECT id FROM users WHERE email = 'rhonda.keller@lablink.edu'), '2026-09-30 12:00:00', 'Approved by lab manager'),
    ((SELECT r.id FROM reservations r JOIN users u ON u.id = r.user_id WHERE u.email = 'liam.foster@lablink.edu' AND r.start_time = '2026-10-06 11:00:00'), 'PENDING', 'APPROVED', (SELECT id FROM users WHERE email = 'marcus.hale@lablink.edu'), '2026-09-30 17:00:00', 'Approved by lab manager'),
    ((SELECT r.id FROM reservations r JOIN users u ON u.id = r.user_id WHERE u.email = 'ava.morgan@lablink.edu' AND r.start_time = '2026-10-12 14:00:00'), NULL, 'PENDING', (SELECT id FROM users WHERE email = 'ava.morgan@lablink.edu'), '2026-10-03 09:00:00', 'Submitted for approval'),
    ((SELECT r.id FROM reservations r JOIN users u ON u.id = r.user_id WHERE u.email = 'ava.morgan@lablink.edu' AND r.start_time = '2026-10-12 14:00:00'), 'PENDING', 'APPROVED', (SELECT id FROM users WHERE email = 'rhonda.keller@lablink.edu'), '2026-10-04 00:00:00', 'Approved by lab manager'),
    ((SELECT r.id FROM reservations r JOIN users u ON u.id = r.user_id WHERE u.email = 'lucas.bennett@lablink.edu' AND r.start_time = '2026-10-08 11:00:00'), NULL, 'PENDING', (SELECT id FROM users WHERE email = 'lucas.bennett@lablink.edu'), '2026-10-05 11:00:00', 'Submitted for approval'),
    ((SELECT r.id FROM reservations r JOIN users u ON u.id = r.user_id WHERE u.email = 'lucas.bennett@lablink.edu' AND r.start_time = '2026-10-08 11:00:00'), 'PENDING', 'REJECTED', (SELECT id FROM users WHERE email = 'rhonda.keller@lablink.edu'), '2026-10-05 15:00:00', 'Rejected by lab manager'),
    ((SELECT r.id FROM reservations r JOIN users u ON u.id = r.user_id WHERE u.email = 'olivia.reyes@lablink.edu' AND r.start_time = '2026-10-09 16:00:00'), NULL, 'PENDING', (SELECT id FROM users WHERE email = 'olivia.reyes@lablink.edu'), '2026-10-05 16:00:00', 'Submitted for approval'),
    ((SELECT r.id FROM reservations r JOIN users u ON u.id = r.user_id WHERE u.email = 'olivia.reyes@lablink.edu' AND r.start_time = '2026-10-09 16:00:00'), 'PENDING', 'APPROVED', (SELECT id FROM users WHERE email = 'manager@lablink.edu'), '2026-10-05 20:00:00', 'Approved by lab manager'),
    ((SELECT r.id FROM reservations r JOIN users u ON u.id = r.user_id WHERE u.email = 'student@lablink.edu' AND r.start_time = '2026-10-19 13:00:00'), NULL, 'PENDING', (SELECT id FROM users WHERE email = 'student@lablink.edu'), '2026-10-10 09:00:00', 'Submitted for approval'),
    ((SELECT r.id FROM reservations r JOIN users u ON u.id = r.user_id WHERE u.email = 'student@lablink.edu' AND r.start_time = '2026-10-19 13:00:00'), 'PENDING', 'APPROVED', (SELECT id FROM users WHERE email = 'manager@lablink.edu'), '2026-10-10 18:00:00', 'Approved by lab manager'),
    ((SELECT r.id FROM reservations r JOIN users u ON u.id = r.user_id WHERE u.email = 'ava.morgan@lablink.edu' AND r.start_time = '2026-10-20 08:00:00'), NULL, 'PENDING', (SELECT id FROM users WHERE email = 'ava.morgan@lablink.edu'), '2026-10-12 07:00:00', 'Submitted for approval'),
    ((SELECT r.id FROM reservations r JOIN users u ON u.id = r.user_id WHERE u.email = 'ava.morgan@lablink.edu' AND r.start_time = '2026-10-20 08:00:00'), 'PENDING', 'REJECTED', (SELECT id FROM users WHERE email = 'marcus.hale@lablink.edu'), '2026-10-12 11:00:00', 'Rejected by lab manager'),
    ((SELECT r.id FROM reservations r JOIN users u ON u.id = r.user_id WHERE u.email = 'student@lablink.edu' AND r.start_time = '2026-10-15 08:00:00'), NULL, 'PENDING', (SELECT id FROM users WHERE email = 'student@lablink.edu'), '2026-10-13 08:00:00', 'Submitted for approval'),
    ((SELECT r.id FROM reservations r JOIN users u ON u.id = r.user_id WHERE u.email = 'student@lablink.edu' AND r.start_time = '2026-10-15 08:00:00'), 'PENDING', 'APPROVED', (SELECT id FROM users WHERE email = 'manager@lablink.edu'), '2026-10-13 20:00:00', 'Approved by lab manager'),
    ((SELECT r.id FROM reservations r JOIN users u ON u.id = r.user_id WHERE u.email = 'aiden.brooks@lablink.edu' AND r.start_time = '2026-10-26 08:00:00'), NULL, 'PENDING', (SELECT id FROM users WHERE email = 'aiden.brooks@lablink.edu'), '2026-10-19 06:00:00', 'Submitted for approval'),
    ((SELECT r.id FROM reservations r JOIN users u ON u.id = r.user_id WHERE u.email = 'aiden.brooks@lablink.edu' AND r.start_time = '2026-10-26 08:00:00'), 'PENDING', 'REJECTED', (SELECT id FROM users WHERE email = 'manager@lablink.edu'), '2026-10-19 21:00:00', 'Rejected by lab manager'),
    ((SELECT r.id FROM reservations r JOIN users u ON u.id = r.user_id WHERE u.email = 'student@lablink.edu' AND r.start_time = '2026-10-27 13:00:00'), NULL, 'PENDING', (SELECT id FROM users WHERE email = 'student@lablink.edu'), '2026-10-21 11:00:00', 'Submitted for approval'),
    ((SELECT r.id FROM reservations r JOIN users u ON u.id = r.user_id WHERE u.email = 'student@lablink.edu' AND r.start_time = '2026-10-27 13:00:00'), 'PENDING', 'APPROVED', (SELECT id FROM users WHERE email = 'marcus.hale@lablink.edu'), '2026-10-21 16:00:00', 'Approved by lab manager'),
    ((SELECT r.id FROM reservations r JOIN users u ON u.id = r.user_id WHERE u.email = 'sarah.whitfield@lablink.edu' AND r.start_time = '2026-11-09 16:00:00'), NULL, 'APPROVED', (SELECT id FROM users WHERE email = 'sarah.whitfield@lablink.edu'), '2026-11-01 14:00:00', 'Submitted and approved automatically'),
    ((SELECT r.id FROM reservations r JOIN users u ON u.id = r.user_id WHERE u.email = 'isabella.ward@lablink.edu' AND r.start_time = '2026-12-07 09:00:00'), NULL, 'PENDING', (SELECT id FROM users WHERE email = 'isabella.ward@lablink.edu'), '2026-11-01 16:00:00', 'Submitted for approval'),
    ((SELECT r.id FROM reservations r JOIN users u ON u.id = r.user_id WHERE u.email = 'jordan.ellis@lablink.edu' AND r.start_time = '2026-12-09 15:00:00'), NULL, 'PENDING', (SELECT id FROM users WHERE email = 'jordan.ellis@lablink.edu'), '2026-11-01 19:00:00', 'Submitted for approval'),
    ((SELECT r.id FROM reservations r JOIN users u ON u.id = r.user_id WHERE u.email = 'grace.okafor@lablink.edu' AND r.start_time = '2026-12-16 09:00:00'), NULL, 'APPROVED', (SELECT id FROM users WHERE email = 'grace.okafor@lablink.edu'), '2026-11-01 19:00:00', 'Submitted and approved automatically'),
    ((SELECT r.id FROM reservations r JOIN users u ON u.id = r.user_id WHERE u.email = 'liam.foster@lablink.edu' AND r.start_time = '2026-12-18 10:00:00'), NULL, 'PENDING', (SELECT id FROM users WHERE email = 'liam.foster@lablink.edu'), '2026-11-01 19:00:00', 'Submitted for approval'),
    ((SELECT r.id FROM reservations r JOIN users u ON u.id = r.user_id WHERE u.email = 'caleb.wright@lablink.edu' AND r.start_time = '2026-12-28 13:00:00'), NULL, 'PENDING', (SELECT id FROM users WHERE email = 'caleb.wright@lablink.edu'), '2026-11-01 20:00:00', 'Submitted for approval'),
    ((SELECT r.id FROM reservations r JOIN users u ON u.id = r.user_id WHERE u.email = 'jordan.ellis@lablink.edu' AND r.start_time = '2026-11-25 11:00:00'), NULL, 'PENDING', (SELECT id FROM users WHERE email = 'jordan.ellis@lablink.edu'), '2026-11-01 20:00:00', 'Submitted for approval'),
    ((SELECT r.id FROM reservations r JOIN users u ON u.id = r.user_id WHERE u.email = 'zoe.hamilton@lablink.edu' AND r.start_time = '2026-12-01 16:00:00'), NULL, 'PENDING', (SELECT id FROM users WHERE email = 'zoe.hamilton@lablink.edu'), '2026-11-01 21:00:00', 'Submitted for approval'),
    ((SELECT r.id FROM reservations r JOIN users u ON u.id = r.user_id WHERE u.email = 'zoe.hamilton@lablink.edu' AND r.start_time = '2026-11-30 09:00:00'), NULL, 'PENDING', (SELECT id FROM users WHERE email = 'zoe.hamilton@lablink.edu'), '2026-11-01 22:00:00', 'Submitted for approval'),
    ((SELECT r.id FROM reservations r JOIN users u ON u.id = r.user_id WHERE u.email = 'marcus.hale@lablink.edu' AND r.start_time = '2027-01-11 15:00:00'), NULL, 'APPROVED', (SELECT id FROM users WHERE email = 'marcus.hale@lablink.edu'), '2026-11-01 23:00:00', 'Submitted and approved automatically'),
    ((SELECT r.id FROM reservations r JOIN users u ON u.id = r.user_id WHERE u.email = 'isabella.ward@lablink.edu' AND r.start_time = '2026-12-07 09:00:00'), 'PENDING', 'CANCELLED', (SELECT id FROM users WHERE email = 'isabella.ward@lablink.edu'), '2026-11-02 00:00:00', 'Cancelled by requester'),
    ((SELECT r.id FROM reservations r JOIN users u ON u.id = r.user_id WHERE u.email = 'ethan.carter@lablink.edu' AND r.start_time = '2027-01-14 15:00:00'), NULL, 'PENDING', (SELECT id FROM users WHERE email = 'ethan.carter@lablink.edu'), '2026-11-02 01:00:00', 'Submitted for approval'),
    ((SELECT r.id FROM reservations r JOIN users u ON u.id = r.user_id WHERE u.email = 'sarah.whitfield@lablink.edu' AND r.start_time = '2026-12-10 08:00:00'), NULL, 'APPROVED', (SELECT id FROM users WHERE email = 'sarah.whitfield@lablink.edu'), '2026-11-02 03:00:00', 'Submitted and approved automatically'),
    ((SELECT r.id FROM reservations r JOIN users u ON u.id = r.user_id WHERE u.email = 'isabella.ward@lablink.edu' AND r.start_time = '2026-11-30 10:00:00'), NULL, 'PENDING', (SELECT id FROM users WHERE email = 'isabella.ward@lablink.edu'), '2026-11-02 04:00:00', 'Submitted for approval'),
    ((SELECT r.id FROM reservations r JOIN users u ON u.id = r.user_id WHERE u.email = 'ava.morgan@lablink.edu' AND r.start_time = '2027-01-06 09:00:00'), NULL, 'PENDING', (SELECT id FROM users WHERE email = 'ava.morgan@lablink.edu'), '2026-11-02 07:00:00', 'Submitted for approval'),
    ((SELECT r.id FROM reservations r JOIN users u ON u.id = r.user_id WHERE u.email = 'maya.patel@lablink.edu' AND r.start_time = '2026-12-04 16:00:00'), NULL, 'PENDING', (SELECT id FROM users WHERE email = 'maya.patel@lablink.edu'), '2026-11-02 11:00:00', 'Submitted for approval'),
    ((SELECT r.id FROM reservations r JOIN users u ON u.id = r.user_id WHERE u.email = 'liam.foster@lablink.edu' AND r.start_time = '2026-12-18 10:00:00'), 'PENDING', 'CANCELLED', (SELECT id FROM users WHERE email = 'liam.foster@lablink.edu'), '2026-11-02 13:00:00', 'Cancelled by requester'),
    ((SELECT r.id FROM reservations r JOIN users u ON u.id = r.user_id WHERE u.email = 'ethan.carter@lablink.edu' AND r.start_time = '2027-01-14 15:00:00'), 'PENDING', 'APPROVED', (SELECT id FROM users WHERE email = 'manager@lablink.edu'), '2026-11-02 16:00:00', 'Approved by lab manager'),
    ((SELECT r.id FROM reservations r JOIN users u ON u.id = r.user_id WHERE u.email = 'ava.morgan@lablink.edu' AND r.start_time = '2026-11-16 13:00:00'), NULL, 'PENDING', (SELECT id FROM users WHERE email = 'ava.morgan@lablink.edu'), '2026-11-02 23:00:00', 'Submitted for approval'),
    ((SELECT r.id FROM reservations r JOIN users u ON u.id = r.user_id WHERE u.email = 'ava.morgan@lablink.edu' AND r.start_time = '2026-11-16 13:00:00'), 'PENDING', 'APPROVED', (SELECT id FROM users WHERE email = 'rhonda.keller@lablink.edu'), '2026-11-03 03:00:00', 'Approved by lab manager'),
    ((SELECT r.id FROM reservations r JOIN users u ON u.id = r.user_id WHERE u.email = 'zoe.hamilton@lablink.edu' AND r.start_time = '2026-11-23 15:00:00'), NULL, 'PENDING', (SELECT id FROM users WHERE email = 'zoe.hamilton@lablink.edu'), '2026-11-03 05:00:00', 'Submitted for approval'),
    ((SELECT r.id FROM reservations r JOIN users u ON u.id = r.user_id WHERE u.email = 'liam.foster@lablink.edu' AND r.start_time = '2026-12-02 16:00:00'), NULL, 'PENDING', (SELECT id FROM users WHERE email = 'liam.foster@lablink.edu'), '2026-11-03 09:00:00', 'Submitted for approval'),
    ((SELECT r.id FROM reservations r JOIN users u ON u.id = r.user_id WHERE u.email = 'isabella.ward@lablink.edu' AND r.start_time = '2026-12-14 15:00:00'), NULL, 'PENDING', (SELECT id FROM users WHERE email = 'isabella.ward@lablink.edu'), '2026-11-03 11:00:00', 'Submitted for approval'),
    ((SELECT r.id FROM reservations r JOIN users u ON u.id = r.user_id WHERE u.email = 'noah.simmons@lablink.edu' AND r.start_time = '2026-11-12 13:00:00'), NULL, 'PENDING', (SELECT id FROM users WHERE email = 'noah.simmons@lablink.edu'), '2026-11-03 11:00:00', 'Submitted for approval'),
    ((SELECT r.id FROM reservations r JOIN users u ON u.id = r.user_id WHERE u.email = 'liam.foster@lablink.edu' AND r.start_time = '2026-12-02 16:00:00'), 'PENDING', 'APPROVED', (SELECT id FROM users WHERE email = 'manager@lablink.edu'), '2026-11-03 11:30:00', 'Approved by lab manager'),
    ((SELECT r.id FROM reservations r JOIN users u ON u.id = r.user_id WHERE u.email = 'noah.simmons@lablink.edu' AND r.start_time = '2026-11-12 13:00:00'), 'PENDING', 'APPROVED', (SELECT id FROM users WHERE email = 'marcus.hale@lablink.edu'), '2026-11-03 11:30:00', 'Approved by lab manager');

-- ---------------------------------------------------------------- maintenance_records (21)
-- Rows with no resolved_at are still open (those items are in MAINTENANCE status).
INSERT INTO maintenance_records (equipment_id, reported_by_id, issue, reported_at, resolved_at, resolution_notes) VALUES
    ((SELECT id FROM equipment WHERE serial_number = 'LAP-0004'), (SELECT id FROM users WHERE email = 'faculty@lablink.edu'), 'Battery swells and will not hold a charge', '2026-10-20 13:00:00', NULL, NULL),
    ((SELECT id FROM equipment WHERE serial_number = 'PRN-0003'), (SELECT id FROM users WHERE email = 'student@lablink.edu'), 'Resin tank film punctured', '2026-10-14 10:00:00', NULL, NULL),
    ((SELECT id FROM equipment WHERE serial_number = 'MIC-0002'), (SELECT id FROM users WHERE email = 'jordan.ellis@lablink.edu'), 'Fine focus knob slips', '2026-09-24 17:00:00', NULL, NULL),
    ((SELECT id FROM equipment WHERE serial_number = 'PRN-0001'), (SELECT id FROM users WHERE email = 'jordan.ellis@lablink.edu'), 'Nozzle clogged with PLA', '2026-10-18 16:00:00', '2026-10-22 21:00:00', 'Replaced 0.4 mm nozzle'),
    ((SELECT id FROM equipment WHERE serial_number = 'PRN-0002'), (SELECT id FROM users WHERE email = 'marcus.hale@lablink.edu'), 'Bed adhesion failing on large prints', '2026-10-20 08:00:00', '2026-10-26 08:00:00', 'Cleaned and re-leveled the build plate'),
    ((SELECT id FROM equipment WHERE serial_number = 'LAP-0001'), (SELECT id FROM users WHERE email = 'marcus.hale@lablink.edu'), 'Keyboard missing two keycaps', '2026-09-02 16:00:00', '2026-09-06 17:00:00', 'Replaced keycaps'),
    ((SELECT id FROM equipment WHERE serial_number = 'LAP-0002'), (SELECT id FROM users WHERE email = 'marcus.hale@lablink.edu'), 'Windows update stuck at 0%', '2026-09-11 16:00:00', '2026-09-17 20:00:00', 'Re-imaged the laptop'),
    ((SELECT id FROM equipment WHERE serial_number = 'TAB-0001'), (SELECT id FROM users WHERE email = 'aiden.brooks@lablink.edu'), 'Cracked screen protector', '2026-10-28 12:00:00', '2026-11-03 11:00:00', 'Replaced screen protector'),
    ((SELECT id FROM equipment WHERE serial_number = 'OSC-0001'), (SELECT id FROM users WHERE email = 'sarah.whitfield@lablink.edu'), 'Channel 3 probe reads noisy', '2026-10-04 12:00:00', '2026-10-10 17:00:00', 'Replaced probe'),
    ((SELECT id FROM equipment WHERE serial_number = 'OSC-0002'), (SELECT id FROM users WHERE email = 'thomas.reed@lablink.edu'), 'Firmware out of date', '2026-10-06 11:00:00', '2026-10-07 13:00:00', 'Updated firmware to 00.04.05'),
    ((SELECT id FROM equipment WHERE serial_number = 'MIC-0001'), (SELECT id FROM users WHERE email = 'rhonda.keller@lablink.edu'), 'USB cable intermittent', '2026-10-14 14:00:00', '2026-10-17 15:00:00', 'Replaced USB cable'),
    ((SELECT id FROM equipment WHERE serial_number = 'CAM-0001'), (SELECT id FROM users WHERE email = 'jordan.ellis@lablink.edu'), 'Lens shows dust spots', '2026-08-20 12:00:00', '2026-08-24 12:00:00', 'Lens cleaned'),
    ((SELECT id FROM equipment WHERE serial_number = 'CAM-0002'), (SELECT id FROM users WHERE email = 'manager@lablink.edu'), 'Battery door latch loose', '2026-10-04 16:00:00', '2026-10-09 20:00:00', 'Latch replaced under warranty'),
    ((SELECT id FROM equipment WHERE serial_number = 'CAM-0003'), (SELECT id FROM users WHERE email = 'marcus.hale@lablink.edu'), 'XLR input crackles', '2026-08-27 13:00:00', '2026-08-29 13:00:00', 'Cleaned the input connector'),
    ((SELECT id FROM equipment WHERE serial_number = 'PRJ-0001'), (SELECT id FROM users WHERE email = 'sarah.whitfield@lablink.edu'), 'Lamp hours exceeded', '2026-09-29 10:00:00', '2026-10-05 10:00:00', 'Replaced lamp'),
    ((SELECT id FROM equipment WHERE serial_number = 'VR-0001'), (SELECT id FROM users WHERE email = 'jordan.ellis@lablink.edu'), 'Left controller drifts', '2026-10-28 08:00:00', '2026-11-02 09:00:00', 'Recalibrated controller'),
    ((SELECT id FROM equipment WHERE serial_number = 'VR-0002'), (SELECT id FROM users WHERE email = 'rhonda.keller@lablink.edu'), 'Face cushion worn out', '2026-09-27 17:00:00', '2026-10-02 20:00:00', 'Replaced face cushion'),
    ((SELECT id FROM equipment WHERE serial_number = 'ARD-0003'), (SELECT id FROM users WHERE email = 'marcus.hale@lablink.edu'), 'Board shorted, will not power on', '2026-09-19 11:00:00', '2026-09-24 12:00:00', 'Retired - not worth repairing'),
    ((SELECT id FROM equipment WHERE serial_number = 'CAM-0004'), (SELECT id FROM users WHERE email = 'rhonda.keller@lablink.edu'), 'Tape mechanism jammed', '2026-10-01 08:00:00', '2026-10-04 11:00:00', 'Retired - replaced by HC-X1500'),
    ((SELECT id FROM equipment WHERE serial_number = 'RPI-0002'), (SELECT id FROM users WHERE email = 'rhonda.keller@lablink.edu'), 'Camera module not detected', '2026-09-02 08:00:00', '2026-09-06 12:00:00', 'Reseated ribbon cable'),
    ((SELECT id FROM equipment WHERE serial_number = 'MIC-AUD-01'), (SELECT id FROM users WHERE email = 'chloe.nguyen@lablink.edu'), 'One stand missing its clamp', '2026-08-21 09:00:00', '2026-08-26 11:00:00', 'Ordered and fitted new clamp');
