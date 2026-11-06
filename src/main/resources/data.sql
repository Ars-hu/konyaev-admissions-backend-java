-- =============================================================================
-- Инициализация начальных данных (PostgreSQL)
-- Пароль по умолчанию: admin123
-- =============================================================================

INSERT INTO admin (username, password_hash, role)
VALUES ('admin', '$2a$10$7EqJtq98hPqEX7fNZaFWoO.8H5O1W0aHlFhJ5hG1vW.6R6lJ1wV/C', 'ROLE_ADMIN')
ON CONFLICT (username) DO NOTHING;

INSERT INTO allowed_month ("year", "month")
VALUES
    (2026, 6),
    (2026, 7),
    (2026, 8)
ON CONFLICT DO NOTHING;

INSERT INTO site_settings ("key", "value")
VALUES
    ('advance_hours', '24')
ON CONFLICT DO NOTHING;
