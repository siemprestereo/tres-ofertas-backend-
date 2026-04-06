CREATE TABLE professional_profession_types (
    professional_id BIGINT NOT NULL,
    profession_type VARCHAR(50) NOT NULL,
    PRIMARY KEY (professional_id, profession_type),
    CONSTRAINT fk_ppt_professional FOREIGN KEY (professional_id) REFERENCES app_users(id) ON DELETE CASCADE
);

INSERT INTO professional_profession_types (professional_id, profession_type)
SELECT id, profession_type
FROM app_users
WHERE profession_type IS NOT NULL AND profession_type != '' AND active_role = 'PROFESSIONAL';
