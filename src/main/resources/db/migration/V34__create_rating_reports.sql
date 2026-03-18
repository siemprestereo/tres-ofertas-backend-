CREATE TABLE rating_reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rating_id BIGINT NOT NULL,
    reporter_id BIGINT NOT NULL,
    reason VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    admin_notes VARCHAR(500),
    created_at DATETIME NOT NULL,
    resolved_at DATETIME,
    CONSTRAINT fk_report_rating FOREIGN KEY (rating_id) REFERENCES ratings(id) ON DELETE CASCADE,
    CONSTRAINT fk_report_reporter FOREIGN KEY (reporter_id) REFERENCES app_users(id) ON DELETE CASCADE
);