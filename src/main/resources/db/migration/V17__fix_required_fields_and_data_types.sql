-- V17: Arreglar campos obligatorios y tipos de datos para CV profesional

-- ========== PROFESSIONALS ==========
-- Actualizar NULLs existentes antes de hacer campos obligatorios
UPDATE professionals
SET reputation_score = 0.0
WHERE reputation_score IS NULL;

UPDATE professionals
SET total_ratings = 0
WHERE total_ratings IS NULL;

-- Hacer reputation_score y total_ratings obligatorios con valores por defecto
ALTER TABLE professionals
MODIFY COLUMN reputation_score DOUBLE NOT NULL DEFAULT 0.0;

ALTER TABLE professionals
MODIFY COLUMN total_ratings INT NOT NULL DEFAULT 0;

-- ========== CVS ==========
-- Actualizar descriptions NULL con texto por defecto
UPDATE cvs
SET description = 'Sin descripción'
WHERE description IS NULL OR description = '';

-- Hacer description obligatorio con default
ALTER TABLE cvs
MODIFY COLUMN description VARCHAR(1000) NOT NULL DEFAULT 'Sin descripción';

-- Cambiar reputation_score de DECIMAL a DOUBLE para consistencia
ALTER TABLE cvs
MODIFY COLUMN reputation_score DOUBLE NOT NULL DEFAULT 0.0;

-- ========== WORK_HISTORY ==========
-- Actualizar business_name NULL con valor por defecto
UPDATE work_history
SET business_name = 'Sin especificar'
WHERE business_name IS NULL OR business_name = '';

-- Hacer business_name obligatorio
ALTER TABLE work_history
MODIFY COLUMN business_name VARCHAR(255) NOT NULL;

-- ========== QR_TOKENS ==========
-- Hacer token opcional (puede ser NULL, se usa 'code' principalmente)
ALTER TABLE qr_tokens
MODIFY COLUMN token VARCHAR(64) NULL;

-- ========== APP_USERS ==========
-- Agregar campo phone (si ya existe, comentar esta línea)
--ALTER TABLE app_users
--ADD COLUMN phone VARCHAR(20) NULL AFTER email;