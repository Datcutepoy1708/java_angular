package com.store.service.impl;

import com.store.entity.payment.PaymentTransaction;
import com.store.entity.payment.ProcessingStatus;
import com.store.repository.PaymentTransactionRepository;
import com.store.service.PaymentFailureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentFailureServiceImpl implements PaymentFailureService {

    private static final int MAX_ATTEMPTS = 5;
    private static final int MAX_REASON_LENGTH = 255;

    private final PaymentTransactionRepository paymentTransactionRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markRetryableFailure(Long sepayTransactionId, String failureReason) {
        String externalId = String.valueOf(sepayTransactionId);
        PaymentTransaction transaction = paymentTransactionRepository
                .findByProviderAndExternalTransactionIdForUpdate("SEPAY", externalId)
                .orElse(null);

        if (transaction == null || isFinalized(transaction.getProcessingStatus())) {
            return;
        }

        int nextAttempt = (transaction.getAttemptCount() == null ? 0 : transaction.getAttemptCount()) + 1;
        transaction.setAttemptCount(nextAttempt);
        transaction.setLastAttemptAt(LocalDateTime.now());
        transaction.setFailureReason(truncate(failureReason));
        transaction.setProcessingStatus(nextAttempt >= MAX_ATTEMPTS
                ? ProcessingStatus.REVIEW_REQUIRED
                : ProcessingStatus.FAILED_RETRYABLE);
        paymentTransactionRepository.save(transaction);

        if (nextAttempt >= MAX_ATTEMPTS) {
            log.error("Payment transaction {} exhausted {} reconciliation attempts and requires review",
                    externalId, nextAttempt);
        }
    }

    private boolean isFinalized(ProcessingStatus status) {
        return status == ProcessingStatus.PROCESSED
                || status == ProcessingStatus.RESOLVED
                || status == ProcessingStatus.REVIEW_REQUIRED;
    }

    private String truncate(String value) {
        if (value == null || value.isBlank()) {
            return "Transient reconciliation failure";
        }
        String normalized = value.trim();
        return normalized.length() <= MAX_REASON_LENGTH
                ? normalized
                : normalized.substring(0, MAX_REASON_LENGTH);
    }
}
