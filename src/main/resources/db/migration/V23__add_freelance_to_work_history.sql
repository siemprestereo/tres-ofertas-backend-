-- Agregar columna is_freelance a work_history
ALTER TABLE work_history
ADD COLUMN is_freelance BOOLEAN DEFAULT FALSE NOT NULL;

-- Hacer business_name nullable para permitir freelance sin nombre de empresa
ALTER TABLE work_history
MODIFY COLUMN business_name VARCHAR(255) NULL;

-- Agregar comentario para documentación
ALTER TABLE work_history
MODIFY COLUMN is_freelance BOOLEAN DEFAULT FALSE NOT NULL
COMMENT 'Indica si este trabajo es freelance/autónomo';