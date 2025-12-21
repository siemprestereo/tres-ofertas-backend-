-- Renombrar tabla
ALTER TABLE work_experiences RENAME TO work_history;

-- Renombrar columna cv_id a professional_id
ALTER TABLE work_history CHANGE COLUMN cv_id professional_id BIGINT NOT NULL;

-- PRIMERO eliminar el foreign key constraint
ALTER TABLE work_history DROP FOREIGN KEY fk_we_cv;

-- LUEGO eliminar el índice (ahora sí puede)
DROP INDEX idx_we_cv ON work_history;

-- Agregar constraint nuevo con professional
ALTER TABLE work_history
  ADD CONSTRAINT fk_wh_professional
  FOREIGN KEY (professional_id) REFERENCES professionals(id);

-- Crear nuevo índice
CREATE INDEX idx_wh_professional ON work_history(professional_id);

-- Actualizar constraint de business (eliminar FK + índice + recrear)
ALTER TABLE work_history DROP FOREIGN KEY fk_we_business;
DROP INDEX idx_we_business ON work_history;

ALTER TABLE work_history
  ADD CONSTRAINT fk_wh_business
  FOREIGN KEY (business_id) REFERENCES businesses(id);

CREATE INDEX idx_wh_business ON work_history(business_id);

-- Agregar columna is_active
ALTER TABLE work_history ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;

-- Actualizar is_active basado en end_date
UPDATE work_history SET is_active = FALSE WHERE end_date IS NOT NULL;

-- Crear índice para is_active
CREATE INDEX idx_wh_active ON work_history(is_active);

-- Agregar columna created_at (NUEVA LÍNEA)
ALTER TABLE work_history
  ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;