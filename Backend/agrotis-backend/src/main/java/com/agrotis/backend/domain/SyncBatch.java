package com.agrotis.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "sync_batches")
public class SyncBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String source;

    @Column(name = "external_reference", nullable = false)
    private String externalReference;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SyncStatus status = SyncStatus.PENDING;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "schema_version", nullable = false)
    private int schemaVersion = 1;

    @Column(name = "entity_type", nullable = false)
    private String entityType;

    @Column(name = "payload_hash", nullable = false, length = 64)
    private String payloadHash;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 64)
    private String idempotencyKey;

    @Column(name = "next_attempt_at")
    private LocalDateTime nextAttemptAt;

    protected SyncBatch() {
    }

    public SyncBatch(String source, String externalReference, String entityType, String payload,
                     String payloadHash, String idempotencyKey) {
        this.source = source;
        this.externalReference = externalReference;
        this.entityType = entityType;
        this.payload = payload;
        this.payloadHash = payloadHash;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        this.nextAttemptAt = this.createdAt;
        this.status = SyncStatus.PENDING;
        this.attemptCount = 0;
    }

    public Long getId() {
        return id;
    }

    public String getSource() {
        return source;
    }

    public String getExternalReference() {
        return externalReference;
    }

    public String getEntityType() {
        return entityType;
    }

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public String getPayloadHash() {
        return payloadHash;
    }

    public LocalDateTime getNextAttemptAt() {
        return nextAttemptAt;
    }

    public void setNextAttemptAt(LocalDateTime nextAttemptAt) {
        this.nextAttemptAt = nextAttemptAt;
        this.updatedAt = LocalDateTime.now();
    }

    public void requeue() {
        this.status = SyncStatus.PENDING;
        this.errorMessage = null;
        this.nextAttemptAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public SyncStatus getStatus() {
        return status;
    }

    public void setStatus(SyncStatus status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }

    public String getPayload() {
        return payload;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(int attemptCount) {
        this.attemptCount = attemptCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
