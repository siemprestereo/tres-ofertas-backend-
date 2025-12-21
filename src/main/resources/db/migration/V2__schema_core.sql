-- 1) Businesses (antes restaurants)
CREATE TABLE IF NOT EXISTS businesses (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(120) NOT NULL,
  address VARCHAR(255),
  business_type VARCHAR(50) NOT NULL DEFAULT 'RESTAURANT',
  phone VARCHAR(50),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE INDEX idx_businesses_name ON businesses(name);
CREATE INDEX idx_businesses_type ON businesses(business_type);

-- 2) CV (1:1 con el professional)
CREATE TABLE IF NOT EXISTS cvs (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  waiter_id BIGINT NOT NULL,
  description VARCHAR(1000),
  reputation_score DECIMAL(4,2) NOT NULL DEFAULT 0.00,
  total_ratings INT NOT NULL DEFAULT 0,
  public_slug VARCHAR(64),
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_cvs_waiter FOREIGN KEY (waiter_id) REFERENCES app_users(id),
  CONSTRAINT uq_cvs_waiter UNIQUE (waiter_id),
  CONSTRAINT uq_cvs_public_slug UNIQUE (public_slug)
) ENGINE=InnoDB;

-- 3) Experiencia laboral (N por CV)
CREATE TABLE IF NOT EXISTS work_experiences (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  cv_id BIGINT NOT NULL,
  business_id BIGINT,
  position VARCHAR(80) NOT NULL,
  start_date DATE NOT NULL,
  end_date DATE NULL,
  reference_contact VARCHAR(140),
  CONSTRAINT fk_we_cv FOREIGN KEY (cv_id) REFERENCES cvs(id),
  CONSTRAINT fk_we_business FOREIGN KEY (business_id) REFERENCES businesses(id)
) ENGINE=InnoDB;

CREATE INDEX idx_we_cv ON work_experiences(cv_id);
CREATE INDEX idx_we_business ON work_experiences(business_id);

-- 4) Ratings
CREATE TABLE IF NOT EXISTS ratings (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  client_id BIGINT NOT NULL,
  waiter_id BIGINT NOT NULL,
  business_id BIGINT NOT NULL,
  score TINYINT NOT NULL,
  comment VARCHAR(140),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_r_client FOREIGN KEY (client_id) REFERENCES app_users(id),
  CONSTRAINT fk_r_waiter FOREIGN KEY (waiter_id) REFERENCES app_users(id),
  CONSTRAINT fk_r_business FOREIGN KEY (business_id) REFERENCES businesses(id),
  CONSTRAINT chk_r_score CHECK (score BETWEEN 1 AND 5)
) ENGINE=InnoDB;

CREATE INDEX idx_ratings_waiter_created ON ratings(waiter_id, created_at);
CREATE INDEX idx_ratings_business_created ON ratings(business_id, created_at);

-- 5) QR dinámicos
CREATE TABLE IF NOT EXISTS qr_tokens (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  waiter_id BIGINT NOT NULL,
  token VARCHAR(64) NOT NULL,
  expires_at TIMESTAMP NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_qr_waiter FOREIGN KEY (waiter_id) REFERENCES app_users(id),
  CONSTRAINT uq_qr_token UNIQUE (token)
) ENGINE=InnoDB;

CREATE INDEX idx_qr_waiter_active ON qr_tokens(waiter_id, active);
CREATE INDEX idx_qr_expires_at ON qr_tokens(expires_at);