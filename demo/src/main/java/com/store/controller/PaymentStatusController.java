package com.store.controller;

import com.store.dto.payment.PaymentPollingRequest;
import com.store.dto.payment.PaymentPollingResponse;
import com.store.dto.response.ApiResponse;
import com.store.security.PaymentPollingRateLimiter;
import com.store.service.PaymentPollingService;
import com.store.util.PaymentSecurityUtil;
import com.store.util.ClientIpResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payment Status", description = "Endpoints for customer payment tracking and polling")
public class PaymentStatusController {

    private final PaymentPollingService paymentPollingService;
    private final PaymentPollingRateLimiter paymentPollingRateLimiter;
    private final PaymentSecurityUtil paymentSecurityUtil;
    private final ClientIpResolver clientIpResolver;

    @PostMapping("/status")
    @Operation(summary = "Poll payment status using opaque one-time tracking token")
    public ResponseEntity<ApiResponse<PaymentPollingResponse>> getPaymentStatus(
            @RequestHeader(value = "X-Payment-Tracking-Token", required = false) String headerToken,
            @Valid @RequestBody(required = false) PaymentPollingRequest body,
            HttpServletRequest request
    ) {
        String token = StringUtils.hasText(headerToken) ? headerToken : (body != null ? body.getToken() : null);

        String clientIp = clientIpResolver.resolveClientIp(request);
        String tokenHash = token != null ? paymentSecurityUtil.sha256Hex(token.trim()) : null;
        paymentPollingRateLimiter.checkRateLimit(clientIp, tokenHash);

        PaymentPollingResponse response = paymentPollingService.getPaymentStatus(token);
        return ResponseEntity.ok(ApiResponse.success("Status retrieved successfully", response));
    }
}
