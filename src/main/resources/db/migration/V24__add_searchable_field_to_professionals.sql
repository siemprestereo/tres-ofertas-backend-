ALTER TABLE professionals
ADD COLUMN searchable BOOLEAN NOT NULL DEFAULT false;

-- Índice para optimizar búsquedas
CREATE INDEX idx_professionals_searchable ON professionals(searchable);

-- Comentario explicativo
COMMENT ON COLUMN professionals.searchable IS 'Indica si el profesional quiere aparecer en búsquedas públicas';