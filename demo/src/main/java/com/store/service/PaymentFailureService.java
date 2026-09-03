package com.store.service;

public interface PaymentFailureService {

    void markRetryableFailure(Long sepayTransactionId, String failureReason);
}
