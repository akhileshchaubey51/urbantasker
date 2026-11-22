-- =========================================================
-- UrbanTasker - SQLite Database Schema & Reference Data
-- Database Engine: SQLite (file: urbantasker.db)
-- Zero Configuration - Self Initializing via Spring Boot
-- =========================================================

-- 1. ADMIN TABLE
CREATE TABLE IF NOT EXISTS admin (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    email TEXT NOT NULL UNIQUE,
    password TEXT NOT NULL,
    role TEXT DEFAULT 'admin',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. USERS TABLE
CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    username TEXT NOT NULL UNIQUE,
    email TEXT NOT NULL UNIQUE,
    mobile TEXT NOT NULL,
    password TEXT NOT NULL,
    role TEXT DEFAULT 'user', -- 'user', 'provider', 'admin'
    security_question TEXT,
    security_answer TEXT,
    status TEXT DEFAULT 'ACTIVE', -- 'ACTIVE', 'BLOCKED'
    address TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 3. SERVICES TABLE
CREATE TABLE IF NOT EXISTS services (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    service_name TEXT NOT NULL UNIQUE,
    category TEXT DEFAULT 'Home Improvement',
    description TEXT,
    base_price REAL DEFAULT 299.00,
    image TEXT,
    is_active INTEGER DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 4. PROVIDERS TABLE
CREATE TABLE IF NOT EXISTS providers (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NULL,
    provider_name TEXT NOT NULL,
    service_type TEXT NOT NULL,
    service_details TEXT,
    address TEXT,
    city TEXT DEFAULT 'New Delhi',
    experience_years INTEGER DEFAULT 3,
    hourly_rate REAL DEFAULT 350.00,
    rating REAL DEFAULT 4.8,
    total_reviews INTEGER DEFAULT 0,
    is_available INTEGER DEFAULT 1,
    photo_file TEXT,
    aadhaar_file TEXT,
    police_file TEXT,
    address_proof_file TEXT,
    status TEXT DEFAULT 'Pending', -- 'Pending', 'Approved', 'Rejected', 'Suspended'
    reject_reason TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

-- 5. BOOKINGS TABLE
CREATE TABLE IF NOT EXISTS bookings (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NULL,
    user_name TEXT NOT NULL,
    service_id INTEGER NULL,
    service_name TEXT NOT NULL,
    provider_id INTEGER NOT NULL,
    provider_name TEXT,
    booking_date TEXT NOT NULL,
    booking_time TEXT NOT NULL,
    service_address TEXT NOT NULL,
    contact_number TEXT,
    instructions TEXT,
    estimated_price REAL DEFAULT 0.00,
    payment_mode TEXT DEFAULT 'CASH', -- 'CASH', 'ONLINE'
    payment_status TEXT DEFAULT 'PENDING', -- 'PENDING', 'PAID', 'REFUNDED'
    status TEXT DEFAULT 'Pending', -- 'Pending', 'Accepted', 'Rejected', 'Completed', 'Cancelled'
    reject_reason TEXT,
    cancellation_reason TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (provider_id) REFERENCES providers(id) ON DELETE CASCADE,
    FOREIGN KEY (service_id) REFERENCES services(id) ON DELETE SET NULL
);

-- 6. REVIEWS TABLE
CREATE TABLE IF NOT EXISTS reviews (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    booking_id INTEGER NOT NULL,
    customer_id INTEGER NULL,
    customer_name TEXT NOT NULL,
    provider_id INTEGER NOT NULL,
    rating INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE,
    FOREIGN KEY (provider_id) REFERENCES providers(id) ON DELETE CASCADE
);

-- 7. COMPLAINTS TABLE
CREATE TABLE IF NOT EXISTS complaints (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NULL,
    user_name TEXT NOT NULL,
    booking_id INTEGER NULL,
    subject TEXT NOT NULL,
    description TEXT NOT NULL,
    status TEXT DEFAULT 'OPEN', -- 'OPEN', 'IN_PROGRESS', 'RESOLVED'
    admin_response TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 8. NOTIFICATIONS TABLE
CREATE TABLE IF NOT EXISTS notifications (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    title TEXT,
    message TEXT NOT NULL,
    type TEXT DEFAULT 'BOOKING',
    is_read INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 9. ADMIN ACTIVITY LOGS TABLE
CREATE TABLE IF NOT EXISTS admin_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    admin_email TEXT NOT NULL,
    action TEXT NOT NULL,
    details TEXT,
    ip_address TEXT DEFAULT '127.0.0.1',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
