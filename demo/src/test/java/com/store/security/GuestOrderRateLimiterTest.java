package com.store.security;

import com.store.entity.order.OrderStatus;
import com.store.exception.RateLimitException;
import com.store.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GuestOrderRateLimiterTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private GuestOrderRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("Should pass rate limit check when IP and Phone are clean")
    void testCheckRateLimit_Clean_Success() {
        when(valueOperations.get("order:ratelimit:guest:burst:127.0.0.1")).thenReturn(null);
        when(valueOperations.get("order:ratelimit:guest:ip:127.0.0.1")).thenReturn("2");
        when(orderRepository.countByReceiverPhoneAndOrderStatusAndCreatedAtAfter(
                eq("0988888888"), eq(OrderStatus.PENDING), any(LocalDateTime.class)))
                .thenReturn(1L);

        assertThatCode(() -> rateLimiter.checkRateLimit("127.0.0.1", "0988888888"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should throw RateLimitException when burst lock is still active (<30s)")
    void testCheckRateLimit_BurstLock_ThrowsRateLimitException() {
        when(valueOperations.get("order:ratelimit:guest:burst:127.0.0.1")).thenReturn("1");

        assertThatThrownBy(() -> rateLimiter.checkRateLimit("127.0.0.1", "0988888888"))
                .isInstanceOf(RateLimitException.class)
                .hasMessageContaining("Thao tác quá nhanh");
    }

    @Test
    @DisplayName("Should throw RateLimitException when hourly order count reaches maximum (5 orders)")
    void testCheckRateLimit_HourlyLimitExceeded_ThrowsRateLimitException() {
        when(valueOperations.get("order:ratelimit:guest:burst:127.0.0.1")).thenReturn(null);
        when(valueOperations.get("order:ratelimit:guest:ip:127.0.0.1")).thenReturn("5");

        assertThatThrownBy(() -> rateLimiter.checkRateLimit("127.0.0.1", "0988888888"))
                .isInstanceOf(RateLimitException.class)
                .hasMessageContaining("đạt giới hạn 5 đơn hàng vãng lai trong 1 giờ");
    }

    @Test
    @DisplayName("Should throw RateLimitException when receiver phone has >= 5 pending orders")
    void testCheckRateLimit_PhonePendingExceeded_ThrowsRateLimitException() {
        when(valueOperations.get("order:ratelimit:guest:burst:127.0.0.1")).thenReturn(null);
        when(valueOperations.get("order:ratelimit:guest:ip:127.0.0.1")).thenReturn("1");
        when(orderRepository.countByReceiverPhoneAndOrderStatusAndCreatedAtAfter(
                eq("0988888888"), eq(OrderStatus.PENDING), any(LocalDateTime.class)))
                .thenReturn(5L);

        assertThatThrownBy(() -> rateLimiter.checkRateLimit("127.0.0.1", "0988888888"))
                .isInstanceOf(RateLimitException.class)
                .hasMessageContaining("Số điện thoại này hiện đang có 5 đơn hàng chờ xác nhận");
    }
}
