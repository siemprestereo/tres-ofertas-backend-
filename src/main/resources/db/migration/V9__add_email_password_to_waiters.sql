-- Agregar columnas email y password a la tabla waiters
ALTER TABLE waiters ADD COLUMN email VARCHAR(255);
ALTER TABLE waiters ADD COLUMN password VARCHAR(255);

-- Crear índice único para email
CREATE UNIQUE INDEX idx_waiters_email ON waiters(email);

-- Actualizar meseros existentes con emails temporales (para testing)
UPDATE waiters SET email = CONCAT('waiter', id, '@example.com') WHERE email IS NULL;
UPDATE waiters SET password = '$2a$10$dummyHashForExistingWaiters' WHERE password IS NULL;

-- Hacer las columnas NOT NULL después de agregar datos
ALTER TABLE waiters MODIFY COLUMN email VARCHAR(255) NOT NULL;
ALTER TABLE waiters MODIFY COLUMN password VARCHAR(255) NOT NULL;