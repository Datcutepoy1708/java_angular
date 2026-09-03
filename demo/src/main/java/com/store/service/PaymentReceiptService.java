package com.store.service;

import com.store.dto.payment.SePayWebhookRequest;

public interface PaymentReceiptService {

    /**
     * Atomically inserts a payment transaction record if it has not been received before.
     * Uses REQUIRES_NEW propagation to commit or roll back independently.
     *
     * @param request the SePay webhook request payload
     * @param rawPayload the raw JSON payload
     * @return true if inserted successfully; false if duplicate key constraint encountered
     */
    boolean insertIfAbsent(SePayWebhookRequest request, String rawPayload);
}
