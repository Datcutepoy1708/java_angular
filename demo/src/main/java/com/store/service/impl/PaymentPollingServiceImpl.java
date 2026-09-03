package com.store.service.impl;

import com.store.dto.payment.PaymentPollingResponse;
import com.store.entity.order.Order;
import com.store.entity.order.PaymentStatus;
import com.store.exception.ResourceNotFoundException;
import com.store.repository.OrderRepository;
import com.store.service.PaymentPollingService;
import com.store.util.PaymentSecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentPollingServiceImpl implements PaymentPollingService {

    private final OrderRepository orderRepository;
    private final PaymentSecurityUtil paymentSecurityUtil;

    @Override
    @Transactional(readOnly = true)
    public PaymentPollingResponse getPaymentStatus(String rawToken) {
        if (!StringUtils.hasText(rawToken)) {
            throw new ResourceNotFoundException("Invalid or missing payment tracking token");
        }

        String hash = paymentSecurityUtil.sha256Hex(rawToken.trim());
        Order order = orderRepository.findByPaymentPollingTokenHash(hash)
                .orElseThrow(() -> new ResourceNotFoundException("Payment tracking token not found or invalid"));

        if (order.getPaymentPollingExpiresAt() != null && order.getPaymentPollingExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResourceNotFoundException("Payment tracking token has expired");
        }

        boolean isPaid = order.getPaymentStatus() == PaymentStatus.PAID;
        return PaymentPollingResponse.builder()
                .status(isPaid ? "PAID" : "PENDING")
                .paidAt(isPaid ? order.getPaidAt() : null)
                .build();
    }
}
