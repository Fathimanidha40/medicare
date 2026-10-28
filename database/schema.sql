-- =====================================================================
-- MEDICARE DATABASE SCHEMA (MySQL 8.0+)
-- Normalized tables with primary keys, foreign keys, indexes, and constraints
-- =====================================================================

CREATE DATABASE IF NOT EXISTS medicare_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE medicare_db;

-- 1. USERS TABLE
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    phone VARCHAR(25),
    date_of_birth DATE,
    emergency_contact VARCHAR(100),
    timezone VARCHAR(50) DEFAULT 'UTC',
    role VARCHAR(20) NOT NULL DEFAULT 'ROLE_USER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. DOCTORS DIRECTORY TABLE
CREATE TABLE IF NOT EXISTS doctors (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    specialty VARCHAR(100) NOT NULL,
    qualification VARCHAR(150) NOT NULL,
    clinic_name VARCHAR(150) NOT NULL,
    address VARCHAR(255) NOT NULL,
    contact_number VARCHAR(30),
    consultation_fee DECIMAL(10,2) DEFAULT 0.00,
    avatar_url VARCHAR(255),
    available_days VARCHAR(100) DEFAULT 'MON,TUE,WED,THU,FRI',
    slot_start_time TIME DEFAULT '09:00:00',
    slot_end_time TIME DEFAULT '17:00:00',
    slot_duration_minutes INT DEFAULT 30,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_doctor_specialty (specialty)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. APPOINTMENTS TABLE
CREATE TABLE IF NOT EXISTS appointments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    doctor_id BIGINT NOT NULL,
    appointment_date_time DATETIME NOT NULL,
    appointment_type VARCHAR(20) NOT NULL DEFAULT 'IN_PERSON',
    reason VARCHAR(255) NOT NULL,
    notes TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'BOOKED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_app_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_app_doctor FOREIGN KEY (doctor_id) REFERENCES doctors(id) ON DELETE CASCADE,
    INDEX idx_app_user (user_id),
    INDEX idx_app_doctor_time (doctor_id, appointment_date_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. MEDICINES TABLE
CREATE TABLE IF NOT EXISTS medicines (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(120) NOT NULL,
    dosage VARCHAR(50) NOT NULL,
    unit VARCHAR(20) DEFAULT 'mg',
    form VARCHAR(30) NOT NULL DEFAULT 'TABLET',
    instructions VARCHAR(255),
    food_timing VARCHAR(30) DEFAULT 'AFTER_FOOD',
    start_date DATE NOT NULL,
    end_date DATE,
    frequency VARCHAR(50) NOT NULL DEFAULT 'DAILY',
    prescribing_doctor VARCHAR(120),
    total_stock INT DEFAULT 0,
    quantity_per_dose INT DEFAULT 1,
    low_stock_threshold INT DEFAULT 5,
    color_tag VARCHAR(20) DEFAULT '#1977cc',
    notes TEXT,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_med_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_med_user_active (user_id, active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5. MEDICINE SCHEDULES TABLE
CREATE TABLE IF NOT EXISTS medicine_schedules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    medicine_id BIGINT NOT NULL,
    time_of_day TIME NOT NULL,
    days_of_week VARCHAR(50) DEFAULT 'ALL',
    interval_days INT DEFAULT 1,
    reminder_active BOOLEAN DEFAULT TRUE,
    cycle_start_date DATE,
    cycle_end_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sched_med FOREIGN KEY (medicine_id) REFERENCES medicines(id) ON DELETE CASCADE,
    INDEX idx_sched_medicine (medicine_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 6. SCHEDULED DOSES (Daily instances)
CREATE TABLE IF NOT EXISTS scheduled_doses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    medicine_id BIGINT NOT NULL,
    schedule_id BIGINT,
    scheduled_date_time DATETIME NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    action_timestamp DATETIME,
    snooze_until DATETIME,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_dose_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_dose_med FOREIGN KEY (medicine_id) REFERENCES medicines(id) ON DELETE CASCADE,
    CONSTRAINT fk_dose_sched FOREIGN KEY (schedule_id) REFERENCES medicine_schedules(id) ON DELETE SET NULL,
    UNIQUE KEY uq_med_scheduled_time (medicine_id, scheduled_date_time),
    INDEX idx_dose_user_date (user_id, scheduled_date_time, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 7. DOSE LOGS (Audit trail of doses taken/skipped)
CREATE TABLE IF NOT EXISTS dose_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    scheduled_dose_id BIGINT,
    user_id BIGINT NOT NULL,
    medicine_id BIGINT NOT NULL,
    scheduled_time DATETIME NOT NULL,
    response_time DATETIME NOT NULL,
    action VARCHAR(20) NOT NULL,
    stock_deducted INT DEFAULT 0,
    notes VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_log_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_log_med FOREIGN KEY (medicine_id) REFERENCES medicines(id) ON DELETE CASCADE,
    INDEX idx_log_user_time (user_id, scheduled_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 8. PRESCRIPTIONS TABLE
CREATE TABLE IF NOT EXISTS prescriptions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(150) NOT NULL,
    doctor_name VARCHAR(120),
    prescription_date DATE NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    stored_file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(50) NOT NULL,
    file_size BIGINT NOT NULL,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_presc_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_presc_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 9. REFILL HISTORY TABLE
CREATE TABLE IF NOT EXISTS refill_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    medicine_id BIGINT NOT NULL,
    quantity_added INT NOT NULL,
    previous_stock INT NOT NULL,
    new_stock INT NOT NULL,
    refill_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    notes VARCHAR(255),
    CONSTRAINT fk_refill_med FOREIGN KEY (medicine_id) REFERENCES medicines(id) ON DELETE CASCADE,
    INDEX idx_refill_medicine (medicine_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 10. CAREGIVER RELATIONSHIPS TABLE
CREATE TABLE IF NOT EXISTS caregiver_relationships (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    caregiver_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    permissions VARCHAR(100) DEFAULT 'VIEW_DOSES,MISSED_DOSE_ALERTS',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_caregiver_patient FOREIGN KEY (patient_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_caregiver_user FOREIGN KEY (caregiver_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uq_patient_caregiver (patient_id, caregiver_id),
    INDEX idx_caregiver_patient (patient_id),
    INDEX idx_caregiver_user (caregiver_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =====================================================================
-- SEED INITIAL DOCTORS (Demonstration data)
-- =====================================================================
INSERT INTO doctors (name, specialty, qualification, clinic_name, address, contact_number, consultation_fee, available_days, slot_start_time, slot_end_time, slot_duration_minutes)
VALUES
('Dr. Sarah Jenkins, MD', 'Cardiology', 'MD (Cardiology), FACC', 'HeartCare Specialty Clinic', 'Suite 401, Metro Health Tower, New York, NY', '+1 (555) 234-5678', 120.00, 'MON,TUE,WED,THU,FRI', '09:00:00', '16:00:00', 30),
('Dr. Rajan Patel, MBBS, MD', 'General Medicine', 'MBBS, MD (Internal Medicine)', 'City Wellness Center', '124 Park Avenue, Suite 12, Chicago, IL', '+1 (555) 345-6789', 75.00, 'MON,TUE,WED,THU,FRI,SAT', '08:30:00', '17:00:00', 30),
('Dr. Emily Chen, MD', 'Endocrinology & Diabetology', 'MD, FACE - Harvard Med', 'Chen Metabolic Health Institute', '500 Commonwealth Ave, Boston, MA', '+1 (555) 456-7890', 140.00, 'MON,WED,THU', '10:00:00', '16:00:00', 30),
('Dr. Marcus Brody, DO', 'Neurology', 'DO, Board Certified Neurologist', 'NeuroCare Specialists', '88 Medical Plaza, San Francisco, CA', '+1 (555) 567-8901', 150.00, 'TUE,THU,FRI', '09:00:00', '15:00:00', 45),
('Dr. Priya Sharma, MD', 'Pediatrics & Family Care', 'MD, FAAP', 'Sunshine Family Health', '34 Elm Street, Austin, TX', '+1 (555) 678-9012', 85.00, 'MON,TUE,WED,THU,FRI', '09:00:00', '17:30:00', 30)
ON DUPLICATE KEY UPDATE name=name;
