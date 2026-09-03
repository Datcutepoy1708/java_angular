package com.store.service;

import com.store.dto.payment.SePayWebhookRequest;
import com.store.dto.payment.WebhookResult;
import com.store.entity.order.Order;
import com.store.entity.order.OrderStatus;
import com.store.entity.order.PaymentMethod;
import com.store.entity.order.PaymentStatus;
import com.store.entity.order.ReconciliationStatus;
import com.store.entity.payment.PaymentTransaction;
import com.store.entity.payment.ProcessingStatus;
import com.store.repository.OrderRepository;
import com.store.repository.PaymentTransactionRepository;
import com.store.util.PaymentSecurityUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        "jwt.secret=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970",
        "app.bank.account-no=090123456789",
        "app.bank.id=MB",
        "app.bank.account-name=COMPLEXUS TEST",
        "app.sepay.enabled=true",
        "app.sepay.webhook-apikey=test-sepay-apikey-secret"
})
class PaymentConcurrencyIntegrationTest {

    @Autowired
    private PaymentWebhookService paymentWebhookService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    @Autowired
    private PaymentSecurityUtil paymentSecurityUtil;

    private Order testOrder;
    private String paymentReference;

    @BeforeEach
    void setUp() {
        paymentReference = paymentSecurityUtil.generatePaymentReference();
        testOrder = Order.builder()
                .orderCode("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .receiverName("Concurrent Tester")
                .receiverPhone("0988888888")
                .shippingAddress("Concurrency Street, Hanoi")
                .subtotal(new BigDecimal("1000000.00"))
                .totalAmount(new BigDecimal("1000000.00"))
                .paymentMethod(PaymentMethod.BANK_TRANSFER)
                .paymentStatus(PaymentStatus.UNPAID)
                .paymentReference(paymentReference)
                .paidAmount(BigDecimal.ZERO)
                .reconciliationStatus(ReconciliationStatus.PENDING)
                .orderStatus(OrderStatus.PENDING)
                .build();
        testOrder = orderRepository.saveAndFlush(testOrder);
    }

    @AfterEach
    void tearDown() {
        if (testOrder != null && testOrder.getOrderId() != null) {
            paymentTransactionRepository.deleteAll(paymentTransactionRepository.findByOrderId(testOrder.getOrderId()));
            orderRepository.deleteById(testOrder.getOrderId());
        }
        paymentTransactionRepository.findByProviderAndExternalTransactionId("SEPAY", "999901")
                .ifPresent(paymentTransactionRepository::delete);
        paymentTransactionRepository.findByProviderAndExternalTransactionId("SEPAY", "999902")
                .ifPresent(paymentTransactionRepository::delete);
        paymentTransactionRepository.findByProviderAndExternalTransactionId("SEPAY", "999903")
                .ifPresent(paymentTransactionRepository::delete);
    }

    @Test
    @DisplayName("True Concurrency: 2 threads submitting identical transaction simultaneously produce 1 receipt and exactly 1 credit")
    void testConcurrentDuplicateWebhook_AtomicReceipt_NoOvercredit() throws Exception {
        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch readyLatch = new CountDownLatch(threads);
        CountDownLatch startLatch = new CountDownLatch(1);

        Long transactionId = 999901L;
        SePayWebhookRequest req = SePayWebhookRequest.builder()
                .id(transactionId)
                .gateway("MBBank")
                .accountNumber("090123456789")
                .transferType("in")
                .transferAmount(new BigDecimal("1000000.00"))
                .content("Thanh toan don hang " + paymentReference)
                .transactionDate("2026-09-04 10:00:00")
                .build();
        String rawPayload = "{\"id\":999901,\"amount\":1000000}";

        List<Callable<WebhookResult>> tasks = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            tasks.add(() -> {
                readyLatch.countDown();
                // Wait for all threads to be ready
                if (!startLatch.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Timeout waiting for start latch");
                }
                return paymentWebhookService.process(req, rawPayload);
            });
        }

        // Prepare threads
        List<Future<WebhookResult>> futures = new ArrayList<>();
        for (Callable<WebhookResult> task : tasks) {
            futures.add(executor.submit(task));
        }

        // Wait until all threads reach latch, then release concurrently
        assertThat(readyLatch.await(5, TimeUnit.SECONDS)).isTrue();
        startLatch.countDown();

        // Collect results
        for (Future<WebhookResult> future : futures) {
            WebhookResult result = future.get(10, TimeUnit.SECONDS);
            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
        }

        executor.shutdown();
        assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();

        // Verify exactly 1 payment transaction exists in DB
        List<PaymentTransaction> txList = paymentTransactionRepository.findByOrderId(testOrder.getOrderId());
        assertThat(txList).hasSize(1);
        PaymentTransaction savedTx = txList.get(0);
        assertThat(savedTx.getExternalTransactionId()).isEqualTo("999901");
        assertThat(savedTx.getProcessingStatus()).isEqualTo(ProcessingStatus.PROCESSED);

        // Verify order is credited exactly once
        Order updatedOrder = orderRepository.findById(testOrder.getOrderId()).orElseThrow();
        assertThat(updatedOrder.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(updatedOrder.getPaidAmount()).isEqualByComparingTo(new BigDecimal("1000000.00"));
        assertThat(updatedOrder.getPaidAt()).isNotNull();
    }

    @Test
    @DisplayName("True Concurrency: 2 distinct transactions for same order simultaneously processed serialize via Pessimistic Lock")
    void testConcurrentDistinctTransactions_PessimisticLock_AccumulatesCorrectly() throws Exception {
        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch readyLatch = new CountDownLatch(threads);
        CountDownLatch startLatch = new CountDownLatch(1);

        SePayWebhookRequest req1 = SePayWebhookRequest.builder()
                .id(999902L)
                .gateway("MBBank")
                .accountNumber("090123456789")
                .transferType("in")
                .transferAmount(new BigDecimal("400000.00"))
                .content("CK 1 " + paymentReference)
                .transactionDate("2026-09-04 10:00:00")
                .build();

        SePayWebhookRequest req2 = SePayWebhookRequest.builder()
                .id(999903L)
                .gateway("MBBank")
                .accountNumber("090123456789")
                .transferType("in")
                .transferAmount(new BigDecimal("600000.00"))
                .content("CK 2 " + paymentReference)
                .transactionDate("2026-09-04 10:00:01")
                .build();

        List<Callable<WebhookResult>> tasks = List.of(
                () -> {
                    readyLatch.countDown();
                    if (!startLatch.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Timeout waiting for start latch");
                    }
                    return paymentWebhookService.process(req1, "{\"id\":999902}");
                },
                () -> {
                    readyLatch.countDown();
                    if (!startLatch.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Timeout waiting for start latch");
                    }
                    return paymentWebhookService.process(req2, "{\"id\":999903}");
                }
        );

        List<Future<WebhookResult>> futures = new ArrayList<>();
        for (Callable<WebhookResult> task : tasks) {
            futures.add(executor.submit(task));
        }

        assertThat(readyLatch.await(5, TimeUnit.SECONDS)).isTrue();
        startLatch.countDown();

        for (Future<WebhookResult> future : futures) {
            WebhookResult result = future.get(10, TimeUnit.SECONDS);
            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
        }

        executor.shutdown();
        assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();

        // Verify both transactions are saved and processed
        List<PaymentTransaction> txList = paymentTransactionRepository.findByOrderId(testOrder.getOrderId());
        assertThat(txList).hasSize(2);

        // Verify cumulative amount is exactly 1,000,000 with no lost updates
        Order updatedOrder = orderRepository.findById(testOrder.getOrderId()).orElseThrow();
        assertThat(updatedOrder.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(updatedOrder.getPaidAmount()).isEqualByComparingTo(new BigDecimal("1000000.00"));
        assertThat(updatedOrder.getPaidAt()).isNotNull();
    }
}
