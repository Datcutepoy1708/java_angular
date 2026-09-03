package com.store.service;

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
import com.store.util.PaymentSecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "jwt.secret=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970",
        "app.bank.account-no=090123456789",
        "app.bank.id=MB",
        "app.bank.account-name=COMPLEXUS TEST",
        "app.sepay.enabled=true",
        "app.sepay.webhook-apikey=test-sepay-apikey-secret"
})
@Transactional
class PaymentReconciliationServiceIntegrationTest {

    @Autowired
    private PaymentReconciliationService reconciliationService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    @Autowired
    private PaymentSecurityUtil paymentSecurityUtil;

    private Order order;
    private String paymentReference;

    @BeforeEach
    void setUp() {
        paymentReference = paymentSecurityUtil.generatePaymentReference();
        order = Order.builder()
                .orderCode("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .receiverName("Tran Thi B")
                .receiverPhone("0912345678")
                .shippingAddress("456 Tran Hung Dao, Da Nang")
                .subtotal(new BigDecimal("1000000.00"))
                .totalAmount(new BigDecimal("1000000.00"))
                .paymentMethod(PaymentMethod.BANK_TRANSFER)
                .paymentStatus(PaymentStatus.UNPAID)
                .paymentReference(paymentReference)
                .paidAmount(BigDecimal.ZERO)
                .reconciliationStatus(ReconciliationStatus.PENDING)
                .orderStatus(OrderStatus.PENDING)
                .build();
        order = orderRepository.saveAndFlush(order);
    }

    @Test
    @DisplayName("Partial payment updates order paidAmount and leaves status UNPAID")
    void testPartialPayment() {
        PaymentTransaction tx = PaymentTransaction.builder()
                .provider("SEPAY")
                .externalTransactionId("5001")
                .transferType(TransferType.IN)
                .accountNumber("090123456789")
                .transferAmount(new BigDecimal("400000.00"))
                .content("CK " + paymentReference)
                .receivedAt(LocalDateTime.now())
                .processingStatus(ProcessingStatus.RECEIVED)
                .build();
        paymentTransactionRepository.saveAndFlush(tx);

        reconciliationService.reconcile(5001L);

        Order updatedOrder = orderRepository.findById(order.getOrderId()).orElseThrow();
        assertThat(updatedOrder.getPaymentStatus()).isEqualTo(PaymentStatus.UNPAID);
        assertThat(updatedOrder.getReconciliationStatus()).isEqualTo(ReconciliationStatus.PARTIAL);
        assertThat(updatedOrder.getPaidAmount()).isEqualByComparingTo(new BigDecimal("400000.00"));

        PaymentTransaction updatedTx = paymentTransactionRepository.findByProviderAndExternalTransactionId("SEPAY", "5001").orElseThrow();
        assertThat(updatedTx.getReconciliationResult()).isEqualTo(ReconciliationResult.PARTIAL);
        assertThat(updatedTx.getProcessingStatus()).isEqualTo(ProcessingStatus.PROCESSED);
    }

    @Test
    @DisplayName("Multiple cumulative payments that reach totalAmount update order to PAID")
    void testCumulativePayments_ReachingTotal() {
        // First partial payment
        PaymentTransaction tx1 = PaymentTransaction.builder()
                .provider("SEPAY")
                .externalTransactionId("6001")
                .transferType(TransferType.IN)
                .accountNumber("090123456789")
                .transferAmount(new BigDecimal("600000.00"))
                .content("CK dot 1 " + paymentReference)
                .receivedAt(LocalDateTime.now())
                .processingStatus(ProcessingStatus.RECEIVED)
                .build();
        paymentTransactionRepository.saveAndFlush(tx1);
        reconciliationService.reconcile(6001L);

        // Second payment completing total
        PaymentTransaction tx2 = PaymentTransaction.builder()
                .provider("SEPAY")
                .externalTransactionId("6002")
                .transferType(TransferType.IN)
                .accountNumber("090123456789")
                .transferAmount(new BigDecimal("400000.00"))
                .content("CK dot 2 " + paymentReference)
                .receivedAt(LocalDateTime.now())
                .processingStatus(ProcessingStatus.RECEIVED)
                .build();
        paymentTransactionRepository.saveAndFlush(tx2);
        reconciliationService.reconcile(6002L);

        Order updatedOrder = orderRepository.findById(order.getOrderId()).orElseThrow();
        assertThat(updatedOrder.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(updatedOrder.getPaidAt()).isNotNull();
        assertThat(updatedOrder.getReconciliationStatus()).isEqualTo(ReconciliationStatus.MATCHED_EXACT);
        assertThat(updatedOrder.getPaidAmount()).isEqualByComparingTo(new BigDecimal("1000000.00"));
    }

    @Test
    @DisplayName("Unknown transferType is ignored and order remains UNPAID")
    void testUnknownTransferType_Ignored() {
        PaymentTransaction tx = PaymentTransaction.builder()
                .provider("SEPAY")
                .externalTransactionId("6003")
                .transferType(TransferType.UNKNOWN)
                .accountNumber("090123456789")
                .transferAmount(new BigDecimal("1000000.00"))
                .content("CK " + paymentReference)
                .receivedAt(LocalDateTime.now())
                .processingStatus(ProcessingStatus.RECEIVED)
                .build();
        paymentTransactionRepository.saveAndFlush(tx);
        reconciliationService.reconcile(6003L);

        Order updatedOrder = orderRepository.findById(order.getOrderId()).orElseThrow();
        assertThat(updatedOrder.getPaymentStatus()).isEqualTo(PaymentStatus.UNPAID);
        assertThat(updatedOrder.getPaidAt()).isNull();

        PaymentTransaction updatedTx = paymentTransactionRepository.findByProviderAndExternalTransactionId("SEPAY", "6003").orElseThrow();
        assertThat(updatedTx.getReconciliationResult()).isEqualTo(ReconciliationResult.IGNORED);
        assertThat(updatedTx.getProcessingStatus()).isEqualTo(ProcessingStatus.PROCESSED);
    }

    @Test
    @DisplayName("Overpayment updates status to PAID and flags REVIEW_REQUIRED")
    void testOverpayment() {
        PaymentTransaction tx = PaymentTransaction.builder()
                .provider("SEPAY")
                .externalTransactionId("7001")
                .transferType(TransferType.IN)
                .accountNumber("090123456789")
                .transferAmount(new BigDecimal("1500000.00"))
                .content("CK du tien " + paymentReference)
                .receivedAt(LocalDateTime.now())
                .processingStatus(ProcessingStatus.RECEIVED)
                .build();
        paymentTransactionRepository.saveAndFlush(tx);

        reconciliationService.reconcile(7001L);

        Order updatedOrder = orderRepository.findById(order.getOrderId()).orElseThrow();
        assertThat(updatedOrder.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(updatedOrder.getReconciliationStatus()).isEqualTo(ReconciliationStatus.OVERPAID);
        assertThat(updatedOrder.getPaidAmount()).isEqualByComparingTo(new BigDecimal("1500000.00"));

        PaymentTransaction updatedTx = paymentTransactionRepository.findByProviderAndExternalTransactionId("SEPAY", "7001").orElseThrow();
        assertThat(updatedTx.getReconciliationResult()).isEqualTo(ReconciliationResult.OVERPAID);
        assertThat(updatedTx.getProcessingStatus()).isEqualTo(ProcessingStatus.REVIEW_REQUIRED);
    }

    @Test
    @DisplayName("Cancelled order receiving transfer is IGNORED with REVIEW_REQUIRED")
    void testCancelledOrder_ReceivesTransfer() {
        order.setOrderStatus(OrderStatus.CANCELLED);
        orderRepository.saveAndFlush(order);

        PaymentTransaction tx = PaymentTransaction.builder()
                .provider("SEPAY")
                .externalTransactionId("8001")
                .transferType(TransferType.IN)
                .accountNumber("090123456789")
                .transferAmount(new BigDecimal("1000000.00"))
                .content("CK " + paymentReference)
                .receivedAt(LocalDateTime.now())
                .processingStatus(ProcessingStatus.RECEIVED)
                .build();
        paymentTransactionRepository.saveAndFlush(tx);

        reconciliationService.reconcile(8001L);

        PaymentTransaction updatedTx = paymentTransactionRepository.findByProviderAndExternalTransactionId("SEPAY", "8001").orElseThrow();
        assertThat(updatedTx.getReconciliationResult()).isEqualTo(ReconciliationResult.IGNORED);
        assertThat(updatedTx.getProcessingStatus()).isEqualTo(ProcessingStatus.REVIEW_REQUIRED);
        assertThat(updatedTx.getFailureReason()).contains("cancelled");
    }

    @Test
    @DisplayName("COD order receiving bank transfer is IGNORED with REVIEW_REQUIRED")
    void testCodOrder_ReceivesTransfer() {
        order.setPaymentMethod(PaymentMethod.COD);
        orderRepository.saveAndFlush(order);

        PaymentTransaction tx = PaymentTransaction.builder()
                .provider("SEPAY")
                .externalTransactionId("8002")
                .transferType(TransferType.IN)
                .accountNumber("090123456789")
                .transferAmount(new BigDecimal("1000000.00"))
                .content("CK " + paymentReference)
                .receivedAt(LocalDateTime.now())
                .processingStatus(ProcessingStatus.RECEIVED)
                .build();
        paymentTransactionRepository.saveAndFlush(tx);

        reconciliationService.reconcile(8002L);

        PaymentTransaction updatedTx = paymentTransactionRepository.findByProviderAndExternalTransactionId("SEPAY", "8002").orElseThrow();
        assertThat(updatedTx.getReconciliationResult()).isEqualTo(ReconciliationResult.IGNORED);
        assertThat(updatedTx.getProcessingStatus()).isEqualTo(ProcessingStatus.REVIEW_REQUIRED);
    }

    @Test
    @DisplayName("Outbound transfer is IGNORED")
    void testOutboundTransfer_Ignored() {
        PaymentTransaction tx = PaymentTransaction.builder()
                .provider("SEPAY")
                .externalTransactionId("8003")
                .transferType(TransferType.OUT)
                .accountNumber("090123456789")
                .transferAmount(new BigDecimal("1000000.00"))
                .content("CK tien ra " + paymentReference)
                .receivedAt(LocalDateTime.now())
                .processingStatus(ProcessingStatus.RECEIVED)
                .build();
        paymentTransactionRepository.saveAndFlush(tx);

        reconciliationService.reconcile(8003L);

        PaymentTransaction updatedTx = paymentTransactionRepository.findByProviderAndExternalTransactionId("SEPAY", "8003").orElseThrow();
        assertThat(updatedTx.getReconciliationResult()).isEqualTo(ReconciliationResult.IGNORED);
        assertThat(updatedTx.getProcessingStatus()).isEqualTo(ProcessingStatus.PROCESSED);
        assertThat(updatedTx.getFailureReason()).contains("not IN");
    }

    @Test
    @DisplayName("Mismatched beneficiary account number is IGNORED")
    void testMismatchedAccount_Ignored() {
        PaymentTransaction tx = PaymentTransaction.builder()
                .provider("SEPAY")
                .externalTransactionId("8004")
                .transferType(TransferType.IN)
                .accountNumber("999999999999") // different from 090123456789
                .transferAmount(new BigDecimal("1000000.00"))
                .content("CK " + paymentReference)
                .receivedAt(LocalDateTime.now())
                .processingStatus(ProcessingStatus.RECEIVED)
                .build();
        paymentTransactionRepository.saveAndFlush(tx);

        reconciliationService.reconcile(8004L);

        PaymentTransaction updatedTx = paymentTransactionRepository.findByProviderAndExternalTransactionId("SEPAY", "8004").orElseThrow();
        assertThat(updatedTx.getReconciliationResult()).isEqualTo(ReconciliationResult.IGNORED);
        assertThat(updatedTx.getProcessingStatus()).isEqualTo(ProcessingStatus.PROCESSED);
        assertThat(updatedTx.getFailureReason()).contains("mismatch");
    }
}
