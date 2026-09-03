package com.store.scheduler;

import com.store.entity.payment.PaymentTransaction;
import com.store.entity.payment.ProcessingStatus;
import com.store.repository.PaymentTransactionRepository;
import com.store.service.PaymentReconciliationService;
import com.store.service.PaymentFailureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.sepay.enabled", havingValue = "true")
public class PaymentRecoveryScheduler {

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PaymentReconciliationService paymentReconciliationService;
    private final PaymentFailureService paymentFailureService;

    private static final int MAX_ATTEMPTS = 5;
    private static final int STALE_THRESHOLD_MINUTES = 2;

    @Scheduled(fixedDelay = 60000, initialDelay = 30000)
    public void recoverPendingPayments() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(STALE_THRESHOLD_MINUTES);
        List<PaymentTransaction> pendingTransactions = paymentTransactionRepository
                .findByProcessingStatusInAndReceivedAtBeforeAndAttemptCountLessThan(
                        List.of(ProcessingStatus.RECEIVED, ProcessingStatus.FAILED_RETRYABLE),
                        cutoff,
                        MAX_ATTEMPTS
                );

        if (pendingTransactions.isEmpty()) {
            return;
        }

        log.info("Found {} pending or retryable payment transactions for recovery", pendingTransactions.size());
        for (PaymentTransaction tx : pendingTransactions) {
            try {
                log.info("Attempting recovery for transaction {} (attempt: {})", tx.getExternalTransactionId(), tx.getAttemptCount());
                paymentReconciliationService.reconcile(Long.parseLong(tx.getExternalTransactionId()));
            } catch (Exception e) {
                log.error("Recovery failed for transaction {}: {}", tx.getExternalTransactionId(), e.getMessage());
                try {
                    paymentFailureService.markRetryableFailure(
                            Long.parseLong(tx.getExternalTransactionId()), e.getMessage());
                } catch (Exception markFailureException) {
                    log.error("Unable to persist recovery failure for transaction {}: {}",
                            tx.getExternalTransactionId(), markFailureException.getMessage());
                }
            }
        }
    }
}
