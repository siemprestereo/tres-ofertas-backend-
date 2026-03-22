ALTER TABLE professions ADD COLUMN category VARCHAR(100) NULL;

UPDATE professions SET category = 'Gastronomía' WHERE code IN ('WAITER','CHEF','BARTENDER','BARISTA');
UPDATE professions SET category = 'Hogar y mantenimiento' WHERE code IN ('ELECTRICIAN','PLUMBER','PAINTER','CARPENTER','CONSTRUCTION_WORKER','GARDENER','AIR_CONDITIONING_TECHNICIAN','CLEANER');
UPDATE professions SET category = 'Salud y bienestar' WHERE code IN ('PILATES');
UPDATE professions SET category = 'Belleza' WHERE code IN ('HAIRDRESSER');
UPDATE professions SET category = 'Servicios' WHERE code IN ('MECHANIC','DRIVER','SECURITY','RECEPTIONIST');
