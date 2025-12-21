-- Renombrar columna waiter_id a professional_id
ALTER TABLE qr_tokens CHANGE COLUMN waiter_id professional_id BIGINT NOT NULL;

-- PRIMERO eliminar el foreign key constraint
ALTER TABLE qr_tokens DROP FOREIGN KEY fk_qr_waiter;

-- LUEGO eliminar el índice (ahora sí puede)
DROP INDEX idx_qr_waiter_active ON qr_tokens;

-- Agregar constraint nuevo con professional
ALTER TABLE qr_tokens
  ADD CONSTRAINT fk_qr_professional
  FOREIGN KEY (professional_id) REFERENCES professionals(id);

-- Crear nuevo índice
CREATE INDEX idx_qr_professional_active ON qr_tokens(professional_id, active);

-- El constraint y el índice de business_id ya existen desde V4, no hace falta modificarlos