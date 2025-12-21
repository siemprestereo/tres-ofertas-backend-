-- V19: Agregar work_history_id a ratings para asociar calificaciones con lugares de trabajo específicos

ALTER TABLE ratings
ADD COLUMN work_history_id BIGINT NULL AFTER business_id;

-- Agregar índice para mejorar búsquedas
ALTER TABLE ratings
ADD INDEX idx_work_history_id (work_history_id);

-- Agregar foreign key
ALTER TABLE ratings
ADD CONSTRAINT fk_ratings_work_history
FOREIGN KEY (work_history_id)
REFERENCES work_history(id)
ON DELETE SET NULL;