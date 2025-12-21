-- Renombrar tabla waiters a professionals
ALTER TABLE waiters RENAME TO professionals;

-- Agregar columna profession_type (por defecto WAITER para datos existentes)
ALTER TABLE professionals ADD COLUMN profession_type VARCHAR(50) NOT NULL DEFAULT 'WAITER';

-- Renombrar columnas de control de cambios
ALTER TABLE professionals CHANGE COLUMN monthly_restaurant_changes monthly_workplace_changes INT NOT NULL DEFAULT 0;
ALTER TABLE professionals CHANGE COLUMN last_restaurant_change_date last_workplace_change_date DATE NULL;