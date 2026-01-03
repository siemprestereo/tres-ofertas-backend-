-- Agregar columna active_role a app_users
ALTER TABLE app_users ADD COLUMN active_role VARCHAR(20) NOT NULL DEFAULT 'CLIENT';

-- Actualizar los profesionales existentes para que tengan PROFESSIONAL como rol activo
UPDATE app_users
SET active_role = 'PROFESSIONAL'
WHERE user_type = 'Professional';

-- Actualizar los clientes existentes para que tengan CLIENT como rol activo
UPDATE app_users
SET active_role = 'CLIENT'
WHERE user_type = 'Client';