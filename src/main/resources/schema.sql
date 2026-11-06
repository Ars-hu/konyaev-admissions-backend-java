-- =============================================================================
-- Схема базы данных приёмной комиссии (PostgreSQL 16)
-- =============================================================================

-- 1. Администраторы
CREATE TABLE IF NOT EXISTS admin (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL DEFAULT 'ROLE_ADMIN'
);

-- 2. Актуальные заявления абитуриентов
CREATE TABLE IF NOT EXISTS application (
    id BIGSERIAL PRIMARY KEY,
    fio VARCHAR(255) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(100) NOT NULL,
    registration_date VARCHAR(20) NOT NULL,
    registration_time VARCHAR(10) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'pending'
);

CREATE INDEX IF NOT EXISTS idx_app_reg_date ON application (registration_date);
CREATE INDEX IF NOT EXISTS idx_app_phone ON application (phone);
CREATE INDEX IF NOT EXISTS idx_app_email ON application (email);

-- 3. Архивные заявления
CREATE TABLE IF NOT EXISTS archived_application (
    id BIGSERIAL PRIMARY KEY,
    original_id BIGINT NOT NULL,
    fio VARCHAR(255) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(100) NOT NULL,
    registration_date VARCHAR(20) NOT NULL,
    registration_time VARCHAR(10) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    archived_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_arch_reg_date ON archived_application (registration_date);

-- 4. Заблокированные даты
CREATE TABLE IF NOT EXISTS blocked_date (
    id BIGSERIAL PRIMARY KEY,
    date VARCHAR(20) NOT NULL UNIQUE
);

-- 5. Открытые выходные дни
CREATE TABLE IF NOT EXISTS opened_date (
    id BIGSERIAL PRIMARY KEY,
    date VARCHAR(20) NOT NULL UNIQUE
);

-- 6. Разрешенные месяцы для записи
CREATE TABLE IF NOT EXISTS allowed_month (
    id BIGSERIAL PRIMARY KEY,
    "year" INT NOT NULL,
    "month" INT NOT NULL,
    CONSTRAINT uq_allowed_month UNIQUE ("year", "month")
);

-- 7. Индивидуальные настройки слотов
CREATE TABLE IF NOT EXISTS slot_config (
    id BIGSERIAL PRIMARY KEY,
    date VARCHAR(20) NOT NULL,
    time VARCHAR(10) NOT NULL,
    max_capacity INT DEFAULT 3,
    is_blocked BOOLEAN DEFAULT FALSE,
    CONSTRAINT uq_slot_date_time UNIQUE (date, time)
);

CREATE INDEX IF NOT EXISTS idx_slot_date ON slot_config (date);

-- 8. Дневные лимиты записей
CREATE TABLE IF NOT EXISTS daily_limit (
    id BIGSERIAL PRIMARY KEY,
    date VARCHAR(20) NOT NULL UNIQUE,
    max_registrations INT NOT NULL DEFAULT 0
);

-- 9. Глобальные настройки сайта
CREATE TABLE IF NOT EXISTS site_settings (
    id BIGSERIAL PRIMARY KEY,
    "key" VARCHAR(50) NOT NULL UNIQUE,
    "value" VARCHAR(100) NOT NULL
);
