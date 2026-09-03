package com.store.service.impl;

import com.store.dto.payment.SePayWebhookRequest;
import com.store.dto.payment.WebhookResult;
import com.store.entity.payment.PaymentTransaction;
import com.store.entity.payment.ProcessingStatus;
import com.store.repository.PaymentTransactionRepository;
import com.store.service.PaymentReceiptService;
import com.store.service.PaymentReconciliationService;
import com.store.service.PaymentWebhookService;
import com.store.service.PaymentFailureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentWebhookServiceImpl implements PaymentWebhookService {

    private final PaymentReceiptService paymentReceiptService;
    private final PaymentReconciliationService paymentReconciliationService;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PaymentFailureService paymentFailureService;

    @Override
    public WebhookResult process(SePayWebhookRequest request, String rawPayload) {
        // Step 1: Atomic insert with REQUIRES_NEW
        boolean inserted = paymentReceiptService.insertIfAbsent(request, rawPayload);
        if (!inserted) {
            String externalId = String.valueOf(request.getId());
            log.info("Transaction {} already recorded in database. Checking current status...", externalId);
            Optional<PaymentTransaction> existingTxOpt = paymentTransactionRepository.findByProviderAndExternalTransactionId("SEPAY", externalId);
            if (existingTxOpt.isPresent()) {
                PaymentTransaction existingTx = existingTxOpt.get();
                ProcessingStatus status = existingTx.getProcessingStatus();
                if (status == ProcessingStatus.PROCESSED || status == ProcessingStatus.RESOLVED || status == ProcessingStatus.REVIEW_REQUIRED) {
                    log.info("Transaction {} already finalized with status {}, returning duplicate success", externalId, status);
                    return WebhookResult.duplicate();
                }
                log.warn("Transaction {} exists with retryable status {}. Retrying reconciliation immediately.", externalId, status);
            }
        }

        // Step 2: Reconcile in a separate transaction with pessimistic lock
        try {
            paymentReconciliationService.reconcile(request.getId());
            return WebhookResult.success();
        } catch (Exception e) {
            log.error("Error reconciling payment transaction {}: {}", request.getId(), e.getMessage(), e);
            try {
                paymentFailureService.markRetryableFailure(request.getId(), e.getMessage());
            } catch (Exception markFailureException) {
                log.error("Unable to persist reconciliation failure for transaction {}: {}",
                        request.getId(), markFailureException.getMessage(), markFailureException);
            }
            // Re-throw so that temporary DB/system failures return 5xx for SePay retry
            throw e;
        }
    }
}
