package com.agrotis.backend.dto;

import com.agrotis.backend.domain.SyncStatus;
import java.time.LocalDateTime;

public record SyncBatchResponse(Long id, int schemaVersion, String entityType,
                               String source, String externalReference, SyncStatus status,
                               String errorMessage, int attemptCount,
                               LocalDateTime createdAt, LocalDateTime updatedAt) {
}
