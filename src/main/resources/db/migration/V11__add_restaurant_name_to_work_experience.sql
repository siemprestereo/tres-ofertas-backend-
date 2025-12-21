-- Agregar columna para nombre de negocio como texto
ALTER TABLE work_experiences ADD COLUMN business_name VARCHAR(255);

-- Copiar nombres existentes desde la tabla businesses
UPDATE work_experiences we
JOIN businesses b ON we.business_id = b.id
SET we.business_name = b.name;

-- Hacer la columna NOT NULL después de migrar datos
ALTER TABLE work_experiences MODIFY COLUMN business_name VARCHAR(255) NOT NULL;

-- Hacer business_id opcional (para nuevas experiencias con texto libre)
ALTER TABLE work_experiences MODIFY COLUMN business_id BIGINT NULL;