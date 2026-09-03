package com.store.entity.payment;

public enum ProcessingStatus {
    RECEIVED,
    PROCESSING,
    PROCESSED,
    FAILED_RETRYABLE,
    REVIEW_REQUIRED,
    RESOLVED
}
