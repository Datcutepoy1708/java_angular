package com.store.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookResult {

    private boolean success;
    private String message;
    private boolean duplicate;

    public static WebhookResult success() {
        return new WebhookResult(true, "Processed successfully", false);
    }

    public static WebhookResult duplicate() {
        return new WebhookResult(true, "Duplicate transaction", true);
    }

    public static WebhookResult ignored(String reason) {
        return new WebhookResult(true, reason, false);
    }
}
