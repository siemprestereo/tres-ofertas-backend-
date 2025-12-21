ALTER TABLE qr_tokens
  ADD COLUMN business_id BIGINT NULL;

CREATE INDEX idx_qr_tokens_business ON qr_tokens (business_id);

ALTER TABLE qr_tokens
  ADD CONSTRAINT fk_qr_tokens_business
  FOREIGN KEY (business_id) REFERENCES businesses(id);