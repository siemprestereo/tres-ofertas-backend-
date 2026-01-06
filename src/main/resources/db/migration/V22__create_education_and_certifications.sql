-- Tabla de educación
CREATE TABLE education (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    professional_id BIGINT NOT NULL,
    institution VARCHAR(200),
    degree VARCHAR(200),
    start_date DATE,
    end_date DATE,
    currently_studying BOOLEAN DEFAULT FALSE,
    description TEXT,
    CONSTRAINT fk_education_professional FOREIGN KEY (professional_id) REFERENCES professionals(id) ON DELETE CASCADE
);

-- Tabla de certificaciones
CREATE TABLE certifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    professional_id BIGINT NOT NULL,
    name VARCHAR(200),
    issuer VARCHAR(200),
    date_obtained DATE,
    expiry_date DATE,
    CONSTRAINT fk_certification_professional FOREIGN KEY (professional_id) REFERENCES professionals(id) ON DELETE CASCADE
);

-- Índices para mejorar performance
CREATE INDEX idx_education_professional ON education(professional_id);
CREATE INDEX idx_certification_professional ON certifications(professional_id);