-- Sample customers
-- All sample accounts use password "password". The stored value is BCrypt.
INSERT INTO users (full_name, email, password, role) VALUES
('Alex Chen', 'alex.chen@example.com', '$2a$10$/2vaYil0t2Iib3AXAZD2He9uJZdWkkZIeEjr0l0dVKMat0FLJibWq', 'CUSTOMER'),
('Jamie Rivera', 'jamie.rivera@example.com', '$2a$10$/2vaYil0t2Iib3AXAZD2He9uJZdWkkZIeEjr0l0dVKMat0FLJibWq', 'CUSTOMER');

-- Sample trainers
INSERT INTO providers (full_name, email, password, role, specialty) VALUES
('Sarah Kim', 'sarah.kim@fitstudio.com', '$2a$10$/2vaYil0t2Iib3AXAZD2He9uJZdWkkZIeEjr0l0dVKMat0FLJibWq', 'PROVIDER', 'Yoga & Pilates'),
('Mike Torres', 'mike.torres@fitstudio.com', '$2a$10$/2vaYil0t2Iib3AXAZD2He9uJZdWkkZIeEjr0l0dVKMat0FLJibWq', 'PROVIDER', 'Strength & Conditioning'),
('Priya Patel', 'priya.patel@fitstudio.com', '$2a$10$/2vaYil0t2Iib3AXAZD2He9uJZdWkkZIeEjr0l0dVKMat0FLJibWq', 'PROVIDER', 'Spin & Cardio');

-- Sample services (each tied to a provider)
INSERT INTO services (provider_id, name, description, duration_minutes, price) VALUES
(1, 'Yoga Class', 'Group vinyasa flow yoga session', 60, 20.00),
(1, 'Pilates 1-on-1', 'Private pilates session', 45, 55.00),
(2, 'Personal Training', '1-on-1 strength training session', 60, 70.00),
(3, 'Spin Class', 'High-energy indoor cycling class', 45, 18.00);

-- Sample availability slots
INSERT INTO availability_slots (provider_id, service_id, slot_date, start_time, end_time, is_booked) VALUES
(1, 1, CURRENT_DATE + 7, '09:00', '10:00', FALSE),
(1, 2, CURRENT_DATE + 7, '11:00', '11:45', FALSE),
(1, 1, CURRENT_DATE + 7, '13:00', '14:00', FALSE),
(2, 3, CURRENT_DATE + 8, '08:00', '09:00', FALSE),
(2, 3, CURRENT_DATE + 8, '17:00', '18:00', FALSE),
(3, 4, CURRENT_DATE + 9, '06:00', '06:45', FALSE),
(3, 4, CURRENT_DATE + 9, '18:00', '18:45', FALSE);

-- Sample appointment (Alex books Sarah's yoga class)
INSERT INTO appointments (slot_id, user_id, status) VALUES
(1, 1, 'CONFIRMED');

-- Reflect that slot 1 is now booked
UPDATE availability_slots SET is_booked = TRUE WHERE slot_id = 1;
