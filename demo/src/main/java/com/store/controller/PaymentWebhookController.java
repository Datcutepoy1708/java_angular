package com.store.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.store.config.PaymentProperties;
import com.store.dto.payment.SePayWebhookRequest;
import com.store.dto.payment.SePayWebhookResponse;
import com.store.service.PaymentWebhookService;
import com.store.util.PaymentSecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments/webhooks")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payment Webhooks", description = "Endpoints for receiving external payment gateway notifications")
public class PaymentWebhookController {

    private final PaymentWebhookService paymentWebhookService;
    private final PaymentSecurityUtil paymentSecurityUtil;
    private final PaymentProperties paymentProperties;
    private final ObjectMapper objectMapper;

    @PostMapping(value = "/sepay", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Receive bank transfer notifications from SePay")
    public ResponseEntity<SePayWebhookResponse> handleSePayWebhook(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody String rawPayload,
            HttpServletRequest request
    ) {
        // Step 1: Authenticate with constant-time comparison
        String configuredApiKey = paymentProperties.getSepay().getWebhookApikey();
        if (!StringUtils.hasText(authHeader) || !StringUtils.hasText(configuredApiKey)) {
            log.warn("Missing Authorization header or unconfigured SePay API key");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String trimmedHeader = authHeader.trim();
        if (!trimmedHeader.regionMatches(true, 0, "apikey ", 0, 7)) {
            log.warn("Invalid Authorization scheme received (expected 'Apikey')");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String incomingApiKey = trimmedHeader.substring(7).trim();

        if (!paymentSecurityUtil.constantTimeEquals(incomingApiKey, configuredApiKey)) {
            log.warn("Invalid SePay API key received");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Step 2: Parse and validate payload
        SePayWebhookRequest webhookRequest;
        try {
            webhookRequest = objectMapper.readValue(rawPayload, SePayWebhookRequest.class);
        } catch (JsonProcessingException e) {
            log.error("Malformed SePay JSON payload: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        if (webhookRequest.getId() == null || webhookRequest.getTransferAmount() == null) {
            log.warn("Missing required fields in SePay payload (id or transferAmount)");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        // Step 3: Process receipt and reconciliation
        paymentWebhookService.process(webhookRequest, rawPayload);

        // Always return {"success": true} for SePay contract compliance
        return ResponseEntity.ok(new SePayWebhookResponse(true));
    }
}
