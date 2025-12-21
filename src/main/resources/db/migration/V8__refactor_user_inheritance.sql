-- ============================================================
-- Migración V8: Refactorización de usuarios con herencia JOINED
-- ============================================================

-- 1. PRIMERO: Eliminar la columna 'role' si existe (compatible con todas las versiones de MySQL)
SET @column_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'app_users'
      AND COLUMN_NAME = 'role'
);

SET @sql = IF(@column_exists > 0,
    'ALTER TABLE app_users DROP COLUMN role',
    'SELECT "Column role does not exist" AS Info');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. Agregar columna discriminadora 'user_type'
ALTER TABLE app_users
ADD COLUMN user_type VARCHAR(20) NOT NULL DEFAULT 'CLIENT';

-- 3. Agregar nuevos campos a app_users
ALTER TABLE app_users
ADD COLUMN password VARCHAR(255),
ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN provider VARCHAR(20),
ADD COLUMN provider_id VARCHAR(100);

-- 4. Agregar timestamps si no existen
SET @column_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'app_users'
      AND COLUMN_NAME = 'created_at'
);

SET @sql = IF(@column_exists = 0,
    'ALTER TABLE app_users ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP',
    'SELECT "Column created_at already exists" AS Info');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'app_users'
      AND COLUMN_NAME = 'updated_at'
);

SET @sql = IF(@column_exists = 0,
    'ALTER TABLE app_users ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP',
    'SELECT "Column updated_at already exists" AS Info');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 5. Crear tabla 'clients' (hereda de app_users)
CREATE TABLE IF NOT EXISTS clients (
    id BIGINT PRIMARY KEY,
    CONSTRAINT fk_clients_app_users FOREIGN KEY (id)
        REFERENCES app_users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. Crear tabla 'waiters' (hereda de app_users)
CREATE TABLE IF NOT EXISTS waiters (
    id BIGINT PRIMARY KEY,
    monthly_restaurant_changes INT NOT NULL DEFAULT 0,
    last_restaurant_change_date DATE,
    CONSTRAINT fk_waiters_app_users FOREIGN KEY (id)
        REFERENCES app_users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. Asegurar que client_id en ratings es nullable
SET @column_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ratings'
      AND COLUMN_NAME = 'client_id'
);

SET @sql = IF(@column_exists > 0,
    'ALTER TABLE ratings MODIFY COLUMN client_id BIGINT NULL',
    'SELECT "Column client_id does not exist yet" AS Info');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;