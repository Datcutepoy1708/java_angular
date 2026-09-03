package com.store.repository;

import com.store.entity.payment.PaymentTransaction;
import com.store.entity.payment.ProcessingStatus;
import com.store.entity.payment.ReconciliationResult;
import com.store.entity.payment.TransferType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    boolean existsByProviderAndExternalTransactionId(String provider, String externalTransactionId);

    Optional<PaymentTransaction> findByProviderAndExternalTransactionId(String provider, String externalTransactionId);

    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM PaymentTransaction t WHERE t.provider = :provider AND t.externalTransactionId = :externalId")
    Optional<PaymentTransaction> findByProviderAndExternalTransactionIdForUpdate(
            @Param("provider") String provider,
            @Param("externalId") String externalId
    );

    List<PaymentTransaction> findByOrderId(Long orderId);

    List<PaymentTransaction> findByOrderIdAndTransferTypeAndReconciliationResultIn(
            Long orderId,
            TransferType transferType,
            Collection<ReconciliationResult> results
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE PaymentTransaction t SET t.processingStatus = :newStatus, t.attemptCount = t.attemptCount + 1, t.lastAttemptAt = :now WHERE t.id = :id AND t.processingStatus IN (:allowedStatuses)")
    int claimForProcessing(
            @Param("id") Long id,
            @Param("newStatus") ProcessingStatus newStatus,
            @Param("allowedStatuses") Collection<ProcessingStatus> allowedStatuses,
            @Param("now") LocalDateTime now
    );

    @Modifying
    @Query(
            value = "INSERT IGNORE INTO payment_transactions " +
                    "(provider, external_transaction_id, transfer_type, gateway, account_number, transfer_amount, content, reference_code, received_at, transaction_date, processing_status, raw_payload, attempt_count, created_at) " +
                    "VALUES (:provider, :externalId, :transferType, :gateway, :accountNumber, :amount, :content, :referenceCode, :receivedAt, :transactionDate, :processingStatus, :rawPayload, 0, NOW())",
            nativeQuery = true
    )
    int insertIgnoreNative(
            @Param("provider") String provider,
            @Param("externalId") String externalId,
            @Param("transferType") String transferType,
            @Param("gateway") String gateway,
            @Param("accountNumber") String accountNumber,
            @Param("amount") BigDecimal amount,
            @Param("content") String content,
            @Param("referenceCode") String referenceCode,
            @Param("receivedAt") LocalDateTime receivedAt,
            @Param("transactionDate") LocalDateTime transactionDate,
            @Param("processingStatus") String processingStatus,
            @Param("rawPayload") String rawPayload
    );

    List<PaymentTransaction> findByProcessingStatusInAndReceivedAtBeforeAndAttemptCountLessThan(
            Collection<ProcessingStatus> statuses,
            LocalDateTime cutoff,
            int maxAttempts
    );
}
