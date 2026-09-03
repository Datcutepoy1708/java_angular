package com.store.service.impl;

import com.store.config.PaymentProperties;
import com.store.entity.order.Order;
import com.store.entity.order.OrderStatus;
import com.store.entity.order.PaymentMethod;
import com.store.entity.order.PaymentStatus;
import com.store.entity.order.ReconciliationStatus;
import com.store.entity.payment.PaymentTransaction;
import com.store.entity.payment.ProcessingStatus;
import com.store.entity.payment.ReconciliationResult;
import com.store.entity.payment.TransferType;
import com.store.repository.OrderRepository;
import com.store.repository.PaymentTransactionRepository;
import com.store.service.PaymentReconciliationService;
import com.store.util.PaymentSecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentReconciliationServiceImpl implements PaymentReconciliationService {

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final OrderRepository orderRepository;
    private final PaymentProperties paymentProperties;
    private final PaymentSecurityUtil paymentSecurityUtil;

    @Override
    @Transactional
    public void reconcile(Long sepayTransactionId) {
        String externalId = String.valueOf(sepayTransactionId);
        Optional<PaymentTransaction> txOpt = paymentTransactionRepository.findByProviderAndExternalTransactionId("SEPAY", externalId);
        if (txOpt.isEmpty()) {
            log.warn("Payment transaction {} not found for reconciliation", externalId);
            return;
        }

        PaymentTransaction transaction = txOpt.get();
        LocalDateTime now = LocalDateTime.now();

        // Atomic claim: only claim if status is RECEIVED or FAILED_RETRYABLE
        int claimed = paymentTransactionRepository.claimForProcessing(
                transaction.getId(),
                ProcessingStatus.PROCESSING,
                List.of(ProcessingStatus.RECEIVED, ProcessingStatus.FAILED_RETRYABLE),
                now
        );
        if (claimed == 0) {
            log.info("Payment transaction {} is already being processed or finalized (status: {})", externalId, transaction.getProcessingStatus());
            return;
        }

        // The atomic bulk update bypasses the persistence context. Reload the
        // entity so attemptCount/status cannot be overwritten with stale values.
        transaction = paymentTransactionRepository
                .findByProviderAndExternalTransactionId("SEPAY", externalId)
                .orElseThrow(() -> new IllegalStateException(
                        "Claimed payment transaction disappeared: " + externalId));

        // Check 1: Transfer amount must be > 0
            if (transaction.getTransferAmount() == null || transaction.getTransferAmount().compareTo(BigDecimal.ZERO) <= 0) {
                transaction.setReconciliationResult(ReconciliationResult.IGNORED);
                transaction.setFailureReason("Non-positive transfer amount");
                transaction.setProcessingStatus(ProcessingStatus.PROCESSED);
                transaction.setProcessedAt(now);
                paymentTransactionRepository.save(transaction);
                return;
            }

            // Check 2: Transfer type must be strictly 'in'
            if (transaction.getTransferType() != TransferType.IN) {
                transaction.setReconciliationResult(ReconciliationResult.IGNORED);
                transaction.setFailureReason("Transfer type is " + transaction.getTransferType() + ", not IN");
                transaction.setProcessingStatus(ProcessingStatus.PROCESSED);
                transaction.setProcessedAt(now);
                paymentTransactionRepository.save(transaction);
                return;
            }

            // Check 3: Account number verification (if configured)
            String expectedAccountNo = paymentProperties.getBank().getAccountNo();
            if (StringUtils.hasText(expectedAccountNo)) {
                String incomingAccount = normalizeAccountNo(transaction.getAccountNumber());
                String expected = normalizeAccountNo(expectedAccountNo);
                if (!expected.equals(incomingAccount)) {
                    transaction.setReconciliationResult(ReconciliationResult.IGNORED);
                    transaction.setFailureReason("Account number mismatch: expected " +
                            paymentSecurityUtil.maskAccountNumber(expected) + " but got " +
                            paymentSecurityUtil.maskAccountNumber(incomingAccount));
                    transaction.setProcessingStatus(ProcessingStatus.PROCESSED);
                    transaction.setProcessedAt(now);
                    paymentTransactionRepository.save(transaction);
                    return;
                }
            }

            // Check 4: Extract payment reference from content
            String paymentReference = extractPaymentReference(transaction.getContent());
            if (!StringUtils.hasText(paymentReference)) {
                transaction.setReconciliationResult(ReconciliationResult.UNMATCHED);
                transaction.setFailureReason("No matching payment reference syntax found in content");
                transaction.setProcessingStatus(ProcessingStatus.PROCESSED);
                transaction.setProcessedAt(now);
                paymentTransactionRepository.save(transaction);
                return;
            }

            // Check 5: Look up and lock Order via pessimistic write lock
            Optional<Order> orderOpt = orderRepository.findByPaymentReferenceForUpdate(paymentReference.toUpperCase());
            if (orderOpt.isEmpty()) {
                transaction.setReconciliationResult(ReconciliationResult.UNMATCHED);
                transaction.setFailureReason("Order with payment reference " + paymentReference + " not found");
                transaction.setProcessingStatus(ProcessingStatus.PROCESSED);
                transaction.setProcessedAt(now);
                paymentTransactionRepository.save(transaction);
                return;
            }

            Order order = orderOpt.get();
            transaction.setOrderId(order.getOrderId());

            // Check 6: Order eligibility
            if (order.getPaymentMethod() != PaymentMethod.BANK_TRANSFER) {
                transaction.setReconciliationResult(ReconciliationResult.IGNORED);
                transaction.setFailureReason("Order payment method is not BANK_TRANSFER (" + order.getPaymentMethod() + ")");
                transaction.setProcessingStatus(ProcessingStatus.REVIEW_REQUIRED);
                transaction.setProcessedAt(now);
                paymentTransactionRepository.save(transaction);
                return;
            }

            if (order.getOrderStatus() == OrderStatus.CANCELLED) {
                transaction.setReconciliationResult(ReconciliationResult.IGNORED);
                transaction.setFailureReason("Order already cancelled");
                transaction.setProcessingStatus(ProcessingStatus.REVIEW_REQUIRED);
                transaction.setProcessedAt(now);
                paymentTransactionRepository.save(transaction);
                return;
            }

            if (order.getPaymentStatus() == PaymentStatus.PAID) {
                transaction.setReconciliationResult(ReconciliationResult.OVERPAID);
                transaction.setFailureReason("Order already paid");
                transaction.setProcessingStatus(ProcessingStatus.REVIEW_REQUIRED);
                transaction.setProcessedAt(now);
                paymentTransactionRepository.save(transaction);
                return;
            }

            // Check 7: Compute cumulative sum using locked order paidAmount
            BigDecimal currentPaid = order.getPaidAmount() != null ? order.getPaidAmount() : BigDecimal.ZERO;
            BigDecimal cumulativePaid = currentPaid.add(transaction.getTransferAmount());
            order.setPaidAmount(cumulativePaid);

            BigDecimal totalAmount = order.getTotalAmount();
            int comparison = cumulativePaid.compareTo(totalAmount);

            if (comparison == 0) {
                // Exact match
                order.setPaymentStatus(PaymentStatus.PAID);
                if (order.getPaidAt() == null) {
                    order.setPaidAt(now);
                }
                order.setReconciliationStatus(ReconciliationStatus.MATCHED_EXACT);
                transaction.setReconciliationResult(ReconciliationResult.MATCHED_EXACT);
                transaction.setProcessingStatus(ProcessingStatus.PROCESSED);
                log.info("Order {} fully paid with exact amount {}", order.getOrderCode(), cumulativePaid);
            } else if (comparison > 0) {
                // Overpaid
                order.setPaymentStatus(PaymentStatus.PAID);
                if (order.getPaidAt() == null) {
                    order.setPaidAt(now);
                }
                order.setReconciliationStatus(ReconciliationStatus.OVERPAID);
                transaction.setReconciliationResult(ReconciliationResult.OVERPAID);
                transaction.setProcessingStatus(ProcessingStatus.REVIEW_REQUIRED);
                transaction.setFailureReason("Overpaid: expected " + totalAmount + ", received total " + cumulativePaid);
                log.warn("Order {} overpaid: expected {}, received total {}", order.getOrderCode(), totalAmount, cumulativePaid);
            } else {
                // Partial payment
                order.setReconciliationStatus(ReconciliationStatus.PARTIAL);
                transaction.setReconciliationResult(ReconciliationResult.PARTIAL);
                transaction.setProcessingStatus(ProcessingStatus.PROCESSED);
                transaction.setFailureReason("Partial payment: received " + cumulativePaid + " of " + totalAmount);
                log.info("Order {} partially paid: received {} of {}", order.getOrderCode(), cumulativePaid, totalAmount);
            }

            orderRepository.save(order);
            transaction.setProcessedAt(now);
            paymentTransactionRepository.save(transaction);
    }

    private String extractPaymentReference(String content) {
        if (!StringUtils.hasText(content)) {
            return null;
        }
        Matcher matcher = PaymentSecurityUtil.REFERENCE_PATTERN.matcher(content);
        // Find if any match corresponds to an active order
        while (matcher.find()) {
            String candidate = matcher.group();
            if (orderRepository.existsByPaymentReference(candidate.toUpperCase())) {
                return candidate.toUpperCase();
            }
        }
        // If none matches active order directly, fallback to first regex match
        matcher.reset();
        if (matcher.find()) {
            return matcher.group().toUpperCase();
        }
        return null;
    }

    private String normalizeAccountNo(String accountNo) {
        if (accountNo == null) return "";
        return accountNo.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
    }
}
