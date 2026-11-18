ALTER TABLE properties ADD COLUMN producer_id BIGINT;

UPDATE properties p
SET producer_id = (
    SELECT MIN(pp.producer_id)
    FROM producer_properties pp
    WHERE pp.property_id = p.id
    GROUP BY pp.property_id
    HAVING COUNT(*) = 1
);

ALTER TABLE properties
    ADD CONSTRAINT fk_property_producer FOREIGN KEY (producer_id) REFERENCES producers(id);

ALTER TABLE sync_batches ADD COLUMN schema_version INTEGER NOT NULL DEFAULT 1;
ALTER TABLE sync_batches ADD COLUMN entity_type VARCHAR(32) NOT NULL DEFAULT 'LEGACY';
ALTER TABLE sync_batches ADD COLUMN payload_hash VARCHAR(64) NOT NULL DEFAULT 'legacy';
ALTER TABLE sync_batches ADD COLUMN idempotency_key VARCHAR(64);
ALTER TABLE sync_batches ADD COLUMN next_attempt_at TIMESTAMP;

UPDATE sync_batches SET next_attempt_at = created_at WHERE status = 'PENDING';
UPDATE sync_batches SET next_attempt_at = updated_at
WHERE status IN ('FAILED', 'IN_PROGRESS') AND next_attempt_at IS NULL;
UPDATE sync_batches SET idempotency_key = 'legacy-' || CAST(id AS VARCHAR);
ALTER TABLE sync_batches ALTER COLUMN idempotency_key SET NOT NULL;

CREATE UNIQUE INDEX uq_sync_idempotency_key ON sync_batches(idempotency_key);
CREATE INDEX ix_sync_due_work ON sync_batches(status, next_attempt_at, created_at);
