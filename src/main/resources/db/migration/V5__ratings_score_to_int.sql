-- Cambiamos el tipo a INT (mantiene datos)
ALTER TABLE ratings
  MODIFY score INT NOT NULL;

-- (Opcional pero recomendado) Restringimos a 1..5
-- MySQL 8.0.16+ aplica CHECK; si tu versión es más vieja, lo ignorará sin romper.
ALTER TABLE ratings
  ADD CONSTRAINT chk_ratings_score CHECK (score BETWEEN 1 AND 5);
