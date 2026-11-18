package com.agrotis.backend.domain;

public enum SyncStatus {
    PENDING,
    IN_PROGRESS,
    SUCCEEDED,
    FAILED,
    DLQ
}
