package com.store.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.store.dto.payment.PaymentPollingRequest;
import com.store.dto.payment.SePayWebhookRequest;
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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "jwt.secret=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970",
        "app.bank.account-no=090123456789",
        "app.bank.id=MB",
        "app.bank.account-name=COMPLEXUS TEST",
        "app.sepay.enabled=true",
        "app.sepay.webhook-apikey=test-sepay-apikey-secret"
})
@AutoConfigureMockMvc
class PaymentWebhookAndPollingHttpIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    @Autowired
    private PaymentSecurityUtil paymentSecurityUtil;

    private static final String API_KEY = "test-sepay-apikey-secret";
    private Order testOrder;
    private String rawPollingToken;

    @BeforeEach
    void setUp() {
        rawPollingToken = paymentSecurityUtil.generateRawPollingToken();
        String tokenHash = paymentSecurityUtil.sha256Hex(rawPollingToken);
        String paymentRef = paymentSecurityUtil.generatePaymentReference();

        testOrder = Order.builder()
                .orderCode("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .receiverName("Nguyen Van A")
                .receiverPhone("0987654321")
                .shippingAddress("123 Duong Le Loi, TP.HCM")
                .subtotal(new BigDecimal("500000.00"))
                .shippingFee(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("500000.00"))
                .paymentMethod(PaymentMethod.BANK_TRANSFER)
                .paymentStatus(PaymentStatus.UNPAID)
                .paymentReference(paymentRef)
                .paymentPollingTokenHash(tokenHash)
                .paymentPollingExpiresAt(LocalDateTime.now().plusMinutes(30))
                .paidAmount(BigDecimal.ZERO)
                .reconciliationStatus(ReconciliationStatus.PENDING)
                .orderStatus(OrderStatus.PENDING)
                .build();

        testOrder = orderRepository.saveAndFlush(testOrder);
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        if (testOrder != null && testOrder.getOrderId() != null) {
            paymentTransactionRepository.deleteAll(paymentTransactionRepository.findByOrderId(testOrder.getOrderId()));
            orderRepository.deleteById(testOrder.getOrderId());
        }
        paymentTransactionRepository.findByProviderAndExternalTransactionId("SEPAY", "88801")
                .ifPresent(paymentTransactionRepository::delete);
        paymentTransactionRepository.findByProviderAndExternalTransactionId("SEPAY", "88802")
                .ifPresent(paymentTransactionRepository::delete);
        paymentTransactionRepository.findByProviderAndExternalTransactionId("SEPAY", "77701")
                .ifPresent(paymentTransactionRepository::delete);
        paymentTransactionRepository.findByProviderAndExternalTransactionId("SEPAY", "99901")
                .ifPresent(paymentTransactionRepository::delete);
        paymentTransactionRepository.findByProviderAndExternalTransactionId("SEPAY", "99902")
                .ifPresent(paymentTransactionRepository::delete);
    }

    @Test
    @DisplayName("Webhook rejects request without Authorization header with 401")
    void testWebhook_MissingAuth_Returns401() throws Exception {
        SePayWebhookRequest req = SePayWebhookRequest.builder()
                .id(1001L)
                .transferAmount(new BigDecimal("500000.00"))
                .build();

        mockMvc.perform(post("/api/v1/payments/webhooks/sepay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Webhook rejects request with invalid ApiKey with 401")
    void testWebhook_InvalidApiKey_Returns401() throws Exception {
        SePayWebhookRequest req = SePayWebhookRequest.builder()
                .id(1002L)
                .transferAmount(new BigDecimal("500000.00"))
                .build();

        mockMvc.perform(post("/api/v1/payments/webhooks/sepay")
                        .header("Authorization", "Apikey wrong-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Webhook rejects malformed JSON with 400")
    void testWebhook_MalformedJson_Returns400() throws Exception {
        mockMvc.perform(post("/api/v1/payments/webhooks/sepay")
                        .header("Authorization", "Apikey " + API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid-json-body}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Webhook successfully reconciles exact bank transfer payment and updates order to PAID")
    void testWebhook_ValidTransfer_ReconcilesOrder_ReturnsSuccessTrue() throws Exception {
        SePayWebhookRequest req = SePayWebhookRequest.builder()
                .id(99901L)
                .gateway("MBBank")
                .accountNumber("090123456789")
                .transferType("in")
                .transferAmount(new BigDecimal("500000.00"))
                .content("Thanh toan don hang " + testOrder.getPaymentReference())
                .transactionDate("2026-09-04 10:00:00")
                .referenceCode("MB99901")
                .build();

        mockMvc.perform(post("/api/v1/payments/webhooks/sepay")
                        .header("Authorization", "Apikey " + API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        Order updatedOrder = orderRepository.findById(testOrder.getOrderId()).orElseThrow();
        assertThat(updatedOrder.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(updatedOrder.getReconciliationStatus()).isEqualTo(ReconciliationStatus.MATCHED_EXACT);
        assertThat(updatedOrder.getPaidAmount()).isEqualByComparingTo(new BigDecimal("500000.00"));

        PaymentTransaction tx = paymentTransactionRepository.findByProviderAndExternalTransactionId("SEPAY", "99901").orElseThrow();
        assertThat(tx.getProcessingStatus()).isEqualTo(ProcessingStatus.PROCESSED);
        assertThat(tx.getReconciliationResult()).isEqualTo(ReconciliationResult.MATCHED_EXACT);
    }

    @Test
    @DisplayName("Webhook stores an unknown transfer type safely and ignores it instead of returning 500")
    void testWebhook_UnknownTransferType_IsStoredAndIgnored() throws Exception {
        SePayWebhookRequest req = SePayWebhookRequest.builder()
                .id(99902L)
                .gateway("MBBank")
                .accountNumber("090123456789")
                .transferType("credit")
                .transferAmount(new BigDecimal("500000.00"))
                .content("Thanh toan don hang " + testOrder.getPaymentReference())
                .transactionDate("2026-09-04 10:00:00")
                .referenceCode("MB99902")
                .build();

        mockMvc.perform(post("/api/v1/payments/webhooks/sepay")
                        .header("Authorization", "Apikey " + API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        Order unchangedOrder = orderRepository.findById(testOrder.getOrderId()).orElseThrow();
        assertThat(unchangedOrder.getPaymentStatus()).isEqualTo(PaymentStatus.UNPAID);

        PaymentTransaction tx = paymentTransactionRepository
                .findByProviderAndExternalTransactionId("SEPAY", "99902")
                .orElseThrow();
        assertThat(tx.getTransferType()).isEqualTo(TransferType.UNKNOWN);
        assertThat(tx.getProcessingStatus()).isEqualTo(ProcessingStatus.PROCESSED);
        assertThat(tx.getReconciliationResult()).isEqualTo(ReconciliationResult.IGNORED);
    }

    @Test
    @DisplayName("Webhook handles duplicate transaction idempotently and returns success: true")
    void testWebhook_DuplicateWebhook_ReturnsSuccessTrue() throws Exception {
        PaymentTransaction existing = PaymentTransaction.builder()
                .provider("SEPAY")
                .externalTransactionId("88801")
                .transferType(TransferType.IN)
                .transferAmount(new BigDecimal("500000.00"))
                .accountNumber("090123456789")
                .receivedAt(LocalDateTime.now())
                .processingStatus(ProcessingStatus.PROCESSED)
                .reconciliationResult(ReconciliationResult.MATCHED_EXACT)
                .build();
        paymentTransactionRepository.saveAndFlush(existing);

        SePayWebhookRequest req = SePayWebhookRequest.builder()
                .id(88801L)
                .transferAmount(new BigDecimal("500000.00"))
                .build();

        mockMvc.perform(post("/api/v1/payments/webhooks/sepay")
                        .header("Authorization", "Apikey " + API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }

    @Test
    @DisplayName("Webhook handles unmatched payment reference, records UNMATCHED, returns success: true")
    void testWebhook_UnmatchedContent_SavesUnmatched() throws Exception {
        SePayWebhookRequest req = SePayWebhookRequest.builder()
                .id(77701L)
                .gateway("MBBank")
                .accountNumber("090123456789")
                .transferType("in")
                .transferAmount(new BigDecimal("200000.00"))
                .content("Khong co ma don hang hop le")
                .transactionDate("2026-09-04 10:00:00")
                .build();

        mockMvc.perform(post("/api/v1/payments/webhooks/sepay")
                        .header("Authorization", "Apikey " + API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        PaymentTransaction tx = paymentTransactionRepository.findByProviderAndExternalTransactionId("SEPAY", "77701").orElseThrow();
        assertThat(tx.getReconciliationResult()).isEqualTo(ReconciliationResult.UNMATCHED);
        assertThat(tx.getProcessingStatus()).isEqualTo(ProcessingStatus.PROCESSED);
    }

    @Test
    @DisplayName("Polling endpoint returns status without leaking orderCode, totalAmount, or PII")
    void testPolling_ValidToken_ReturnsPending_NoPiiLeak() throws Exception {
        PaymentPollingRequest request = new PaymentPollingRequest(rawPollingToken);

        mockMvc.perform(post("/api/v1/payments/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("PENDING")))
                .andExpect(jsonPath("$.data.paidAt", nullValue()))
                .andExpect(jsonPath("$.data.orderCode").doesNotExist())
                .andExpect(jsonPath("$.data.totalAmount").doesNotExist())
                .andExpect(jsonPath("$.data.receiverName").doesNotExist());
    }

    @Test
    @DisplayName("Polling endpoint with expired token returns 404")
    void testPolling_ExpiredToken_Returns404() throws Exception {
        testOrder.setPaymentPollingExpiresAt(LocalDateTime.now().minusMinutes(5));
        orderRepository.saveAndFlush(testOrder);

        PaymentPollingRequest request = new PaymentPollingRequest(rawPollingToken);

        mockMvc.perform(post("/api/v1/payments/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Polling endpoint with unknown token returns 404")
    void testPolling_UnknownToken_Returns404() throws Exception {
        PaymentPollingRequest request = new PaymentPollingRequest("non-existent-random-token-12345");

        mockMvc.perform(post("/api/v1/payments/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Webhook rejects Bearer scheme with 401 when contract requires Apikey")
    void testWebhook_BearerScheme_Returns401() throws Exception {
        SePayWebhookRequest req = SePayWebhookRequest.builder()
                .id(1003L)
                .transferAmount(new BigDecimal("500000.00"))
                .build();

        mockMvc.perform(post("/api/v1/payments/webhooks/sepay")
                        .header("Authorization", "Bearer " + API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Duplicate webhook re-triggers reconciliation if previous status was FAILED_RETRYABLE")
    void testWebhook_DuplicateWebhook_RetriesReconciliationIfFailedRetryable() throws Exception {
        PaymentTransaction tx = PaymentTransaction.builder()
                .provider("SEPAY")
                .externalTransactionId("88802")
                .transferType(TransferType.IN)
                .gateway("MBBank")
                .accountNumber("090123456789")
                .transferAmount(new BigDecimal("500000.00"))
                .content("Thanh toan " + testOrder.getPaymentReference())
                .receivedAt(LocalDateTime.now())
                .processingStatus(ProcessingStatus.FAILED_RETRYABLE)
                .build();
        paymentTransactionRepository.saveAndFlush(tx);

        SePayWebhookRequest req = SePayWebhookRequest.builder()
                .id(88802L)
                .gateway("MBBank")
                .accountNumber("090123456789")
                .transferType("in")
                .transferAmount(new BigDecimal("500000.00"))
                .content("Thanh toan " + testOrder.getPaymentReference())
                .build();

        mockMvc.perform(post("/api/v1/payments/webhooks/sepay")
                        .header("Authorization", "Apikey " + API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        Order updatedOrder = orderRepository.findById(testOrder.getOrderId()).orElseThrow();
        assertThat(updatedOrder.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(updatedOrder.getPaidAt()).isNotNull();

        PaymentTransaction updatedTx = paymentTransactionRepository.findByProviderAndExternalTransactionId("SEPAY", "88802").orElseThrow();
        assertThat(updatedTx.getProcessingStatus()).isEqualTo(ProcessingStatus.PROCESSED);
    }
}
