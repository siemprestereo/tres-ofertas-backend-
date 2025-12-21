-- 1) Agrego la columna como NULL temporal para poder backfillear
ALTER TABLE qr_tokens
  ADD COLUMN code VARCHAR(64) NULL;

-- 2) Backfill: para filas existentes, genero un código único
UPDATE qr_tokens
SET code = REPLACE(UUID(), '-', '')
WHERE code IS NULL;

-- 3) Ahora sí, marco NOT NULL y creo índice único
ALTER TABLE qr_tokens
  MODIFY code VARCHAR(64) NOT NULL;

CREATE UNIQUE INDEX uq_qr_tokens_code ON qr_tokens(code);
