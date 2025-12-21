-- Renombrar columna waiter_id a professional_id
ALTER TABLE cvs CHANGE COLUMN waiter_id professional_id BIGINT NOT NULL;

-- PRIMERO eliminar el foreign key constraint
ALTER TABLE cvs DROP FOREIGN KEY fk_cvs_waiter;

-- LUEGO eliminar el constraint único (ahora sí puede)
DROP INDEX uq_cvs_waiter ON cvs;

-- Agregar constraint nuevo con professional
ALTER TABLE cvs
  ADD CONSTRAINT fk_cvs_professional
  FOREIGN KEY (professional_id) REFERENCES professionals(id);

-- Crear constraint único para professional_id
ALTER TABLE cvs ADD CONSTRAINT uq_cvs_professional UNIQUE (professional_id);