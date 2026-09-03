package com.store.service.impl;

import com.store.dto.payment.SePayWebhookRequest;
import com.store.entity.payment.ProcessingStatus;
import com.store.entity.payment.TransferType;
import com.store.repository.PaymentTransactionRepository;
import com.store.service.PaymentReceiptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentReceiptServiceImpl implements PaymentReceiptService {

    private final PaymentTransactionRepository paymentTransactionRepository;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean insertIfAbsent(SePayWebhookRequest request, String rawPayload) {
        String externalId = String.valueOf(request.getId());

        // Fail-closed: only strictly valid transferType is accepted, else UNKNOWN
        TransferType transferType = TransferType.UNKNOWN;
        if (request.getTransferType() != null && !request.getTransferType().isBlank()) {
            try {
                transferType = TransferType.fromValue(request.getTransferType());
            } catch (IllegalArgumentException exception) {
                log.warn("Unknown transferType received from SePay; storing transaction as UNKNOWN");
                transferType = TransferType.UNKNOWN;
            }
        }

        LocalDateTime transDate = null;
        if (request.getTransactionDate() != null && !request.getTransactionDate().isBlank()) {
            try {
                transDate = LocalDateTime.parse(request.getTransactionDate().trim(), DATE_TIME_FORMATTER);
            } catch (DateTimeParseException e) {
                log.warn("Unable to parse transactionDate '{}' from SePay: {}", request.getTransactionDate(), e.getMessage());
                transDate = null;
            }
        }

        BigDecimal amount = request.getTransferAmount() != null ? request.getTransferAmount() : BigDecimal.ZERO;
        String gateway = trimToLength(request.getGateway(), 50);
        String accountNumber = trimToLength(request.getAccountNumber(), 50);
        String referenceCode = trimToLength(request.getReferenceCode(), 100);
        String content = trimToLength(request.getContent(), 500);
        String truncatedPayload = trimToLength(rawPayload, 2048);

        int affected = paymentTransactionRepository.insertIgnoreNative(
                "SEPAY",
                externalId,
                transferType.getValue(),
                gateway,
                accountNumber,
                amount,
                content,
                referenceCode,
                LocalDateTime.now(),
                transDate,
                ProcessingStatus.RECEIVED.name(),
                truncatedPayload
        );

        if (affected > 0) {
            log.info("Saved new payment transaction {} from SePay (type: {}, amount: {})", externalId, transferType, amount);
            return true;
        } else {
            log.info("Duplicate payment transaction {} from SePay, skipped via atomic insert", externalId);
            return false;
        }
    }

    private String trimToLength(String val, int maxLength) {
        if (val == null) return null;
        String trimmed = val.trim();
        return trimmed.length() > maxLength ? trimmed.substring(0, maxLength) : trimmed;
    }
}
