package com.store.service;

public interface PaymentReconciliationService {

    /**
     * Reconciles a newly inserted payment transaction against order and banking rules.
     * Uses pessimistic write lock when updating matching orders.
     *
     * @param sepayTransactionId the external transaction ID from SePay
     */
    void reconcile(Long sepayTransactionId);
}
