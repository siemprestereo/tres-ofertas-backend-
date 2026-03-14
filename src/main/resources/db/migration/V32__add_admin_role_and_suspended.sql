ALTER TABLE app_users ADD COLUMN suspended BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE app_users SET active_role = 'ADMIN' WHERE email = 'soporte@calificalo.com.ar';