package com.store.controller;

import com.store.entity.order.Order;
import com.store.entity.order.OrderStatus;
import com.store.entity.order.PaymentMethod;
import com.store.entity.order.PaymentStatus;
import com.store.entity.user.Role;
import com.store.entity.user.User;
import com.store.entity.user.UserStatus;
import com.store.repository.OrderRepository;
import com.store.repository.RoleRepository;
import com.store.repository.UserRepository;
import com.store.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.stringContainsInOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "jwt.secret=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970")
@AutoConfigureMockMvc
@Transactional
class OrderSecurityHttpIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private OrderRepository orderRepository;

    private User userA;
    private User userB;
    private Order orderUserA;
    private Order guestOrder;

    private static RequestPostProcessor customUserDetails(User user) {
        CustomUserDetails principal = new CustomUserDetails(user);
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        return SecurityMockMvcRequestPostProcessors.authentication(auth);
    }

    @BeforeEach
    void setUp() {
        Role customerRole = roleRepository.findByRoleName("ROLE_CUSTOMER")
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .roleName("ROLE_CUSTOMER")
                        .description("Customer role")
                        .build()));

        String suffix = UUID.randomUUID().toString().substring(0, 8);

        userA = userRepository.save(User.builder()
                .fullName("User A")
                .email("userA_" + suffix + "@test.com")
                .phone("091" + suffix.replaceAll("[^0-9]", "0").substring(0, 7))
                .passwordHash("$2a$10$dummyPasswordHashForTestingOnly123456789")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(customerRole))
                .build());

        userB = userRepository.save(User.builder()
                .fullName("User B")
                .email("userB_" + suffix + "@test.com")
                .phone("092" + suffix.replaceAll("[^0-9]", "0").substring(0, 7))
                .passwordHash("$2a$10$dummyPasswordHashForTestingOnly123456789")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(customerRole))
                .build());

        orderUserA = orderRepository.save(Order.builder()
                .orderCode("ORD-USER-A-" + suffix)
                .user(userA)
                .receiverName("Receiver A")
                .receiverPhone("0911222333")
                .shippingAddress("123 Test Street")
                .subtotal(new BigDecimal("1000000.00"))
                .totalAmount(new BigDecimal("1000000.00"))
                .paymentMethod(PaymentMethod.COD)
                .paymentStatus(PaymentStatus.UNPAID)
                .orderStatus(OrderStatus.PENDING)
                .build());

        guestOrder = orderRepository.save(Order.builder()
                .orderCode("ORD-GUEST-" + suffix)
                .user(null)
                .customerEmail("guest_" + suffix + "@test.com")
                .receiverName("Secret Guest Name")
                .receiverPhone("0988776655")
                .shippingAddress("456 Guest Road")
                .subtotal(new BigDecimal("500000.00"))
                .totalAmount(new BigDecimal("500000.00"))
                .paymentMethod(PaymentMethod.COD)
                .paymentStatus(PaymentStatus.UNPAID)
                .orderStatus(OrderStatus.PENDING)
                .build());
    }

    @Test
    @DisplayName("GET /api/v1/orders/{orderCode} as Order Owner returns 200 OK")
    void getOrderByCode_AsOwner_Returns200Ok() throws Exception {
        mockMvc.perform(get("/api/v1/orders/" + orderUserA.getOrderCode())
                        .with(customUserDetails(userA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderCode").value(orderUserA.getOrderCode()));
    }

    @Test
    @DisplayName("GET /api/v1/orders/{orderCode} as Wrong Owner returns 403 Forbidden")
    void getOrderByCode_AsWrongOwner_Returns403Forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/orders/" + orderUserA.getOrderCode())
                        .with(customUserDetails(userB)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("GET /api/v1/orders/{orderCode} as Logged-in Member accessing Guest Order returns 403 Forbidden")
    void getOrderByCode_MemberAccessingGuestOrder_Returns403Forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/orders/" + guestOrder.getOrderCode())
                        .with(customUserDetails(userA)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("GET /api/v1/orders/track with non-existent code returns 404 with uniform error contract")
    void trackGuestOrder_NonExistentCode_ReturnsUniform404() throws Exception {
        mockMvc.perform(get("/api/v1/orders/track")
                        .param("code", "ORD-NON-EXISTENT-999")
                        .param("phone", "0988776655"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Không tìm thấy đơn hàng khớp với mã đơn và số điện thoại đã cung cấp"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("GET /api/v1/orders/track with existing code but wrong phone returns identical 404 without data leakage")
    void trackGuestOrder_ExistingCodeWrongPhone_ReturnsIdenticalUniform404() throws Exception {
        mockMvc.perform(get("/api/v1/orders/track")
                        .param("code", guestOrder.getOrderCode())
                        .param("phone", "0911111111"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Không tìm thấy đơn hàng khớp với mã đơn và số điện thoại đã cung cấp"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(content().string(not(org.hamcrest.Matchers.containsString("Secret Guest Name"))));
    }

    @Test
    @DisplayName("POST /api/v1/orders without auth is publicly accessible (validates payload, not blocked by 401)")
    void guestCheckout_IsPubliclyAccessible() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("GET /api/v1/orders/{orderCode} without auth returns 401 Unauthorized")
    void getOrderByCode_Anonymous_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/orders/ORD-TEST-12345"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/orders/{orderCode}/cancel without auth returns 401 Unauthorized")
    void cancelOrder_Anonymous_Returns401() throws Exception {
        mockMvc.perform(post("/api/v1/orders/ORD-TEST-12345/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Changed mind\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/orders/my-orders without auth returns 401 Unauthorized")
    void getMyOrders_Anonymous_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/orders/my-orders"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/orders/admin without auth returns 401 Unauthorized")
    void getAdminOrders_Anonymous_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/orders/admin"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = {"CUSTOMER"})
    @DisplayName("GET /api/v1/orders/admin with ROLE_CUSTOMER returns 403 Forbidden")
    void getAdminOrders_AsCustomer_Returns403() throws Exception {
        mockMvc.perform(get("/api/v1/orders/admin"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /actuator/health is public for container probes (not blocked by 401)")
    void actuatorHealth_IsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().is(org.hamcrest.Matchers.isOneOf(200, 503)));
    }
}
