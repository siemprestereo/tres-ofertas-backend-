-- Eliminar índice único
DROP INDEX idx_waiters_email ON waiters;

-- Eliminar columna email duplicada
ALTER TABLE waiters DROP COLUMN email;

-- Eliminar columna password duplicada (también está en app_users)
ALTER TABLE waiters DROP COLUMN password;