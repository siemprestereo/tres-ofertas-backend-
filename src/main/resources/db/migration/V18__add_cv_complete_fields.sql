-- V18: Agregar campos para CV profesional completo

-- ========== APP_USERS ==========
-- Agregar ubicación (ciudad, país)
ALTER TABLE app_users
ADD COLUMN location VARCHAR(100) NULL AFTER phone;

-- Agregar título profesional (ej: "Mesero Senior", "Electricista Matriculado")
ALTER TABLE app_users
ADD COLUMN professional_title VARCHAR(100) NULL AFTER location;

-- ========== WORK_HISTORY ==========
-- Agregar descripción del puesto (responsabilidades, logros)
ALTER TABLE work_history
ADD COLUMN description TEXT NULL AFTER position;