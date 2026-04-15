-- ── Users ─────────────────────────────────────────────────────────────────────
CREATE TABLE app_users (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255),
    role          ENUM('ADMIN','MERCHANT','CONSUMER') NOT NULL,
    name          VARCHAR(255),
    google_id     VARCHAR(255),
    active        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── Merchants (locales) ───────────────────────────────────────────────────────
CREATE TABLE merchants (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT NOT NULL,
    name         VARCHAR(255) NOT NULL,
    slug         VARCHAR(100) NOT NULL UNIQUE,
    category     VARCHAR(100) NOT NULL,
    sub_category VARCHAR(100),
    address      VARCHAR(255) NOT NULL,
    lat          DECIMAL(10,7),
    lng          DECIMAL(10,7),
    whatsapp     VARCHAR(30),
    phone        VARCHAR(30),
    email        VARCHAR(255),
    schedule     VARCHAR(500),
    photo_url    VARCHAR(500),
    verified     BOOLEAN NOT NULL DEFAULT FALSE,
    active       BOOLEAN NOT NULL DEFAULT TRUE,
    suspended    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_merchant_user FOREIGN KEY (user_id) REFERENCES app_users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── Offers ────────────────────────────────────────────────────────────────────
CREATE TABLE offers (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_id     BIGINT NOT NULL,
    code            VARCHAR(10) NOT NULL UNIQUE,
    description     VARCHAR(500) NOT NULL,
    price           DECIMAL(10,2),
    photo_url       VARCHAR(500),
    until_stock_out BOOLEAN NOT NULL DEFAULT FALSE,
    expires_at      DATETIME NOT NULL,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_offer_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── Offer view events ─────────────────────────────────────────────────────────
CREATE TABLE offer_views (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    offer_id    BIGINT NOT NULL,
    viewed_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_view_offer FOREIGN KEY (offer_id) REFERENCES offers(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── Merchant profile view events ──────────────────────────────────────────────
CREATE TABLE merchant_views (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_id BIGINT NOT NULL,
    viewed_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_mview_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── Follows (consumer → merchant) ─────────────────────────────────────────────
CREATE TABLE follows (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    consumer_id BIGINT NOT NULL,
    merchant_id BIGINT NOT NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_follow (consumer_id, merchant_id),
    CONSTRAINT fk_follow_consumer FOREIGN KEY (consumer_id) REFERENCES app_users(id),
    CONSTRAINT fk_follow_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── Offer reports ─────────────────────────────────────────────────────────────
CREATE TABLE offer_reports (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    offer_id    BIGINT NOT NULL,
    reporter_id BIGINT NOT NULL,
    reason      ENUM('OFFER_NOT_FOUND','WRONG_PRICE','STORE_CLOSED','OTHER') NOT NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_report_offer    FOREIGN KEY (offer_id)    REFERENCES offers(id),
    CONSTRAINT fk_report_reporter FOREIGN KEY (reporter_id) REFERENCES app_users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── Suspensions ───────────────────────────────────────────────────────────────
CREATE TABLE suspensions (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_id  BIGINT NOT NULL,
    reason       VARCHAR(500),
    suspended_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    lifted_at    DATETIME,
    permanent    BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_suspension_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── Sub-category suggestions ──────────────────────────────────────────────────
CREATE TABLE category_suggestions (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    suggested_by BIGINT NOT NULL,
    name         VARCHAR(255) NOT NULL,
    reviewed     BOOLEAN NOT NULL DEFAULT FALSE,
    approved     BOOLEAN,
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_suggestion_user FOREIGN KEY (suggested_by) REFERENCES app_users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── Banned words ──────────────────────────────────────────────────────────────
CREATE TABLE banned_words (
    id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    word VARCHAR(100) NOT NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── Password reset tokens ─────────────────────────────────────────────────────
CREATE TABLE password_reset_tokens (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT NOT NULL,
    token      VARCHAR(255) NOT NULL UNIQUE,
    expires_at DATETIME NOT NULL,
    used       BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_prt_user FOREIGN KEY (user_id) REFERENCES app_users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
