package com.store.entity.payment;

public enum ReconciliationResult {
    MATCHED_EXACT,
    PARTIAL,
    OVERPAID,
    UNMATCHED,
    IGNORED
}
