-- Renombrar columna waiter_id a professional_id
ALTER TABLE ratings CHANGE COLUMN waiter_id professional_id BIGINT NOT NULL;

-- PRIMERO eliminar el foreign key constraint
ALTER TABLE ratings DROP FOREIGN KEY fk_r_waiter;

-- LUEGO eliminar el índice (ahora sí puede)
DROP INDEX idx_ratings_waiter_created ON ratings;

-- Agregar constraint nuevo con professional
ALTER TABLE ratings
  ADD CONSTRAINT fk_r_professional
  FOREIGN KEY (professional_id) REFERENCES professionals(id);

-- Crear nuevo índice
CREATE INDEX idx_ratings_professional_created ON ratings(professional_id, created_at);

-- La columna business_id, el constraint fk_r_business y el índice idx_ratings_business
-- ya existen desde V2, no hace falta modificarlos

-- Agregar columna service_date
ALTER TABLE ratings ADD COLUMN service_date TIMESTAMP NULL;

-- Crear índice para service_date
CREATE INDEX idx_ratings_service_date ON ratings(service_date);

-- Índice compuesto para consultas por professional + business
CREATE INDEX idx_ratings_prof_business ON ratings(professional_id, business_id);