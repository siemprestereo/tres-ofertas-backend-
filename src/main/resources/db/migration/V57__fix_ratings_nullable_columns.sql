-- Ensure professional_id and client_id are nullable in ratings.
-- V43 and V44 may not have applied correctly in production due to a duplicate V40 conflict.
-- This migration is idempotent: safe to run whether V43/V44 ran or not.

ALTER TABLE ratings MODIFY COLUMN professional_id BIGINT NULL;
ALTER TABLE ratings MODIFY COLUMN client_id BIGINT NULL;

ALTER TABLE ratings DROP FOREIGN KEY fk_ratings_professional;
ALTER TABLE ratings DROP FOREIGN KEY fk_ratings_client;

ALTER TABLE ratings ADD CONSTRAINT fk_ratings_professional
    FOREIGN KEY (professional_id) REFERENCES app_users(id) ON DELETE SET NULL;

ALTER TABLE ratings ADD CONSTRAINT fk_ratings_client
    FOREIGN KEY (client_id) REFERENCES app_users(id) ON DELETE SET NULL;
