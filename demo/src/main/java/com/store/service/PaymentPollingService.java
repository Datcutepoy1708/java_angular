package com.store.service;

import com.store.dto.payment.PaymentPollingResponse;

public interface PaymentPollingService {

    /**
     * Checks payment status for an order identified by a raw one-time polling token.
     * Computes the SHA-256 hash before lookup and ensures the token has not expired.
     *
     * @param rawToken the raw token received from client
     * @return PaymentPollingResponse with status ("PENDING" or "PAID") and paidAt timestamp
     */
    PaymentPollingResponse getPaymentStatus(String rawToken);
}
