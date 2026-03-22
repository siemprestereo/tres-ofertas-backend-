CREATE TABLE professions (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

INSERT INTO professions (code, display_name, active) VALUES
('WAITER', 'Mesero/a', TRUE),
('ELECTRICIAN', 'Electricista', TRUE),
('PLUMBER', 'Plomero/a', TRUE),
('PAINTER', 'Pintor/a', TRUE),
('CARPENTER', 'Carpintero/a', TRUE),
('PILATES', 'Instructora de pilates', TRUE),
('HAIRDRESSER', 'Peluquero/a', TRUE),
('MECHANIC', 'Mecánico/a', TRUE),
('CLEANER', 'Personal de Limpieza', TRUE),
('CHEF', 'Chef/Cocinero', TRUE),
('BARTENDER', 'Bartender', TRUE),
('CONSTRUCTION_WORKER', 'Obrero de Construcción', TRUE),
('GARDENER', 'Jardinero/a', TRUE),
('BARISTA', 'Barista', TRUE),
('DRIVER', 'Conductor', TRUE),
('SECURITY', 'Personal de seguridad', TRUE),
('RECEPTIONIST', 'Recepcionista', TRUE),
('AIR_CONDITIONING_TECHNICIAN', 'Instalador de A.A', TRUE),
('OTHER', 'Otro', TRUE);
