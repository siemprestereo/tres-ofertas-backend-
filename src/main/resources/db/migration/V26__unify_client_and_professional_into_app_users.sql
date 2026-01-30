-- V26: Unify Client and Professional into app_users

-- Step 1: Create new unified table
CREATE TABLE app_users_new (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255),
    phone VARCHAR(20),
    location VARCHAR(255),
    profile_picture VARCHAR(500),
    provider VARCHAR(50),
    provider_id VARCHAR(255),
    email_verified BOOLEAN DEFAULT FALSE,
    
    active_role VARCHAR(20) NOT NULL,
    last_role_switch_at DATETIME,
    
    profession_type VARCHAR(50),
    professional_title VARCHAR(255),
    reputation_score DOUBLE,
    total_ratings INT DEFAULT 0,
    monthly_workplace_changes INT DEFAULT 0,
    last_workplace_change_date DATE,
    searchable BOOLEAN DEFAULT TRUE,
    
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Step 2: Migrate Professionals
INSERT INTO app_users_new (
    id, name, email, password, phone, location, profile_picture, 
    provider, provider_id, email_verified,
    active_role, 
    profession_type, professional_title, reputation_score, total_ratings,
    monthly_workplace_changes, last_workplace_change_date, searchable,
    created_at, updated_at
)
SELECT 
    p.id, p.name, p.email, p.password, p.phone, p.location, p.profile_picture,
    p.provider, p.provider_id, p.email_verified,
    'PROFESSIONAL',
    p.profession_type, p.professional_title, p.reputation_score, p.total_ratings,
    p.monthly_workplace_changes, p.last_workplace_change_date, p.searchable,
    p.created_at, p.updated_at
FROM professionals p
JOIN app_users au ON p.id = au.id;

-- Step 3: Migrate Clients
INSERT INTO app_users_new (
    id, name, email, password, phone, location, profile_picture,
    provider, provider_id, email_verified,
    active_role,
    created_at, updated_at
)
SELECT 
    c.id, c.name, c.email, c.password, c.phone, c.location, c.profile_picture,
    c.provider, c.provider_id, c.email_verified,
    'CLIENT',
    c.created_at, c.updated_at
FROM clients c
JOIN app_users au ON c.id = au.id
WHERE c.id NOT IN (SELECT id FROM app_users_new);

-- Step 4: Backup and replace
RENAME TABLE app_users TO app_users_old_backup;
RENAME TABLE clients TO clients_old_backup;
RENAME TABLE professionals TO professionals_old_backup;
RENAME TABLE app_users_new TO app_users;

-- Step 5: Recreate foreign keys
ALTER TABLE ratings ADD CONSTRAINT fk_ratings_client FOREIGN KEY (client_id) REFERENCES app_users(id) ON DELETE CASCADE;
ALTER TABLE ratings ADD CONSTRAINT fk_ratings_professional FOREIGN KEY (professional_id) REFERENCES app_users(id) ON DELETE CASCADE;
ALTER TABLE cvs ADD CONSTRAINT fk_cvs_professional FOREIGN KEY (professional_id) REFERENCES app_users(id) ON DELETE CASCADE;
ALTER TABLE work_history ADD CONSTRAINT fk_work_history_professional FOREIGN KEY (professional_id) REFERENCES app_users(id) ON DELETE CASCADE;
ALTER TABLE education ADD CONSTRAINT fk_education_professional FOREIGN KEY (professional_id) REFERENCES app_users(id) ON DELETE CASCADE;
ALTER TABLE favorite_professionals ADD CONSTRAINT fk_favorite_client FOREIGN KEY (client_id) REFERENCES app_users(id) ON DELETE CASCADE;
ALTER TABLE favorite_professionals ADD CONSTRAINT fk_favorite_professional FOREIGN KEY (professional_id) REFERENCES app_users(id) ON DELETE CASCADE;
