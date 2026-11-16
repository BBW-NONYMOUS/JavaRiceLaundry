-- ============================================================================
-- Metric Laundry: Digital Laundry Ordering System
-- MySQL / MariaDB database (tested with XAMPP MariaDB 10.4)
--
-- The application runs this script automatically when it starts, so the
-- database is created on first run. You can also import it in phpMyAdmin.
-- Safe to run more than once.
-- ============================================================================

CREATE DATABASE IF NOT EXISTS metric_laundry CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE metric_laundry;

-- Customer records and staff / admin accounts. Passwords are stored as PBKDF2 hashes, never as plain text.
CREATE TABLE IF NOT EXISTS users (
    user_id        INT AUTO_INCREMENT PRIMARY KEY,
    full_name      VARCHAR(100) NOT NULL,
    email          VARCHAR(150) NOT NULL UNIQUE,
    phone          CHAR(11)     NOT NULL,
    password_hash  VARCHAR(120) NOT NULL,
    role           ENUM('Customer', 'Staff', 'Admin') NOT NULL DEFAULT 'Customer',
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE = InnoDB;

-- Databases created before the Admin role was added: allow the 'Admin' role.
ALTER TABLE users MODIFY role ENUM('Customer', 'Staff', 'Admin') NOT NULL DEFAULT 'Customer';

-- Service and pricing list, maintained by the admin.
CREATE TABLE IF NOT EXISTS services (
    service_code     VARCHAR(20)   PRIMARY KEY,
    service_name     VARCHAR(60)   NOT NULL,
    description      VARCHAR(150)  NOT NULL,
    icon             VARCHAR(40)   NOT NULL,
    rate_per_kg      DECIMAL(10,2) NOT NULL,
    turnaround_days  INT           NOT NULL,
    is_available     TINYINT(1)    NOT NULL DEFAULT 1,
    sort_order       INT           NOT NULL
) ENGINE = InnoDB;

-- Laundry order records. The service name and rate are copied at order time,
-- so later rate changes do not alter past charges.
CREATE TABLE IF NOT EXISTS orders (
    order_id             INT AUTO_INCREMENT PRIMARY KEY,
    customer_id          INT           NOT NULL,
    service_code         VARCHAR(20)   NOT NULL,
    service_name         VARCHAR(60)   NOT NULL,
    weight_kg            DECIMAL(6,2)  NOT NULL,
    rate_per_kg          DECIMAL(10,2) NOT NULL,
    total_amount         DECIMAL(10,2) NOT NULL,
    order_date           DATETIME      NOT NULL,
    expected_completion  DATE          NOT NULL,
    order_status         ENUM('Received', 'Washing', 'Drying', 'Ready for Pickup', 'Claimed') NOT NULL DEFAULT 'Received',
    staff_note           VARCHAR(255)  NOT NULL DEFAULT '',
    CONSTRAINT fk_orders_customer FOREIGN KEY (customer_id) REFERENCES users (user_id),
    CONSTRAINT fk_orders_service FOREIGN KEY (service_code) REFERENCES services (service_code),
    INDEX idx_orders_status (order_status),
    INDEX idx_orders_date (order_date)
) ENGINE = InnoDB;

-- Payment records: one per order. GCash / Bank Transfer stay "Pending Verification" until staff verify them.
CREATE TABLE IF NOT EXISTS payments (
    payment_id         INT AUTO_INCREMENT PRIMARY KEY,
    order_id           INT           NOT NULL UNIQUE,
    payment_method     ENUM('Cash', 'GCash', 'Bank Transfer') NOT NULL,
    payment_reference  VARCHAR(30)   NULL,
    amount             DECIMAL(10,2) NOT NULL,
    payment_status     ENUM('Unpaid', 'Pending Verification', 'Paid') NOT NULL,
    paid_at            DATETIME      NULL,
    verified_by        INT           NULL,
    CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders (order_id) ON DELETE CASCADE,
    CONSTRAINT fk_payments_staff FOREIGN KEY (verified_by) REFERENCES users (user_id),
    INDEX idx_payments_status (payment_status)
) ENGINE = InnoDB;

-- Order status records: every status update, when it happened and which staff member recorded it.
CREATE TABLE IF NOT EXISTS order_status_history (
    history_id  INT AUTO_INCREMENT PRIMARY KEY,
    order_id    INT      NOT NULL,
    status      ENUM('Received', 'Washing', 'Drying', 'Ready for Pickup', 'Claimed') NOT NULL,
    changed_at  DATETIME NOT NULL,
    changed_by  INT      NULL,
    CONSTRAINT fk_history_order FOREIGN KEY (order_id) REFERENCES orders (order_id) ON DELETE CASCADE,
    CONSTRAINT fk_history_staff FOREIGN KEY (changed_by) REFERENCES users (user_id),
    INDEX idx_history_order (order_id)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------------
-- Sample data (INSERT IGNORE keeps existing rows when the script is re-run)
-- Customer: juan.dcruz@email.com / password123
-- Staff:    staff@metriclaundry.com / staff123
-- Admin:    admin@metriclaundry.com / admin123   (change this password after the first login)
-- Rates are sample values; set Metric Laundry's actual rates in Services & Rates.
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO users (user_id, full_name, email, phone, password_hash, role) VALUES
    (1, 'Juan Dela Cruz', 'juan.dcruz@email.com', '09123456789',
        'pbkdf2_sha256$120000$TWV0cmljTGF1bmRyeS1jMQ==$vUwp64zyGFwbu0S/sWlw5te3wgx2kbhpxz4wPnl8W64=', 'Customer'),
    (2, 'Metric Laundry Staff', 'staff@metriclaundry.com', '09170000000',
        'pbkdf2_sha256$120000$TWV0cmljTGF1bmRyeS1zMQ==$xScyLnzBljp8OjMw07NN8og27nlVHC3Jk4wO3aA08d4=', 'Staff');

-- Admin (owner) account. No fixed user_id, so it never collides with accounts already registered.
INSERT IGNORE INTO users (full_name, email, phone, password_hash, role) VALUES
    ('Metric Laundry Admin', 'admin@metriclaundry.com', '09170000001',
        'pbkdf2_sha256$120000$TWV0cmljTGF1bmRyeS1hMQ==$o9mwTgiulmbzIXnJTM9VU3Oq65B8LEyRnVRNy47Atwg=', 'Admin');

INSERT IGNORE INTO services (service_code, service_name, description, icon, rate_per_kg, turnaround_days, is_available, sort_order) VALUES
    ('WASH_FOLD', 'Wash and Fold', 'Clean, fresh and neatly folded clothes.', 'clothes.png', 25.00, 1, 1, 1),
    ('DRY_CLEAN', 'Dry Cleaning', 'For delicate and special fabrics.', 'hanger.png', 60.00, 3, 1, 2),
    ('IRONING', 'Ironing', 'Pressed and wrinkle-free clothes (limited types).', 'iron.png', 30.00, 1, 1, 3);
