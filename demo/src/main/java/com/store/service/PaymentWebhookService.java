package com.store.service;

import com.store.dto.payment.SePayWebhookRequest;
import com.store.dto.payment.WebhookResult;

public interface PaymentWebhookService {

    /**
     * Coordinates atomic receipt and subsequent reconciliation of SePay webhook.
     * Non-transactional boundary to preserve independence between insert and reconcile.
     *
     * @param request the parsed webhook request
     * @param rawPayload the raw JSON string
     * @return WebhookResult indicating success, duplicate, or other status
     */
    WebhookResult process(SePayWebhookRequest request, String rawPayload);
}
