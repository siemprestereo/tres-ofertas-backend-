-- Si la columna no existe, la agrego con SQL dinámico
SET @col_exists := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'ratings'
    AND COLUMN_NAME = 'updated_at'
);

SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE ratings ADD COLUMN updated_at TIMESTAMP NULL',
  'SELECT 1'
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Backfill (si está en NULL)
UPDATE ratings
SET updated_at = CURRENT_TIMESTAMP
WHERE updated_at IS NULL;

-- Aseguro NOT NULL + default + auto-update
-- (si ya estaba con ese tipo, no rompe)
ALTER TABLE ratings
  MODIFY updated_at TIMESTAMP NOT NULL
  DEFAULT CURRENT_TIMESTAMP
  ON UPDATE CURRENT_TIMESTAMP;
