-- Normalize legacy profession codes to the current standard codes.
-- MOZO_A was stored before standardization; map it to WAITER.

-- 1. Fix in app_users (legacy single field)
UPDATE app_users SET profession_type = 'WAITER' WHERE profession_type = 'MOZO_A';

-- 2. Fix in professional_profession_types join table
-- First add the canonical code if not already present
INSERT IGNORE INTO professional_profession_types (professional_id, profession_type)
SELECT professional_id, 'WAITER'
FROM professional_profession_types
WHERE profession_type = 'MOZO_A'
  AND NOT EXISTS (
    SELECT 1 FROM professional_profession_types p2
    WHERE p2.professional_id = professional_profession_types.professional_id
      AND p2.profession_type = 'WAITER'
  );

-- Then remove the legacy code
DELETE FROM professional_profession_types WHERE profession_type = 'MOZO_A';
