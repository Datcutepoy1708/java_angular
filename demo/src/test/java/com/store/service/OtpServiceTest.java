package com.store.service;

import com.store.exception.RateLimitException;
import com.store.service.impl.OtpServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private OtpServiceImpl otpService;

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("generateAndSaveOtp should generate 6-digit OTP when cooldown and rate limit pass")
    void generateAndSaveOtp_success() {
        when(redisTemplate.hasKey("auth:otp:cooldown:test@store.com")).thenReturn(false);
        when(valueOperations.increment("auth:otp:hourly:test@store.com")).thenReturn(1L);

        String otp = otpService.generateAndSaveOtp("test@store.com");

        assertThat(otp).isNotNull().hasSize(6).containsOnlyDigits();
        verify(valueOperations).set(eq("auth:otp:test@store.com"), eq(otp), eq(Duration.ofSeconds(300)));
        verify(valueOperations).set(eq("auth:otp:cooldown:test@store.com"), eq("1"), eq(Duration.ofSeconds(60)));
    }

    @Test
    @DisplayName("generateAndSaveOtp should throw RateLimitException when cooldown is active")
    void generateAndSaveOtp_cooldownActive_throws() {
        when(redisTemplate.hasKey("auth:otp:cooldown:test@store.com")).thenReturn(true);
        when(redisTemplate.getExpire("auth:otp:cooldown:test@store.com")).thenReturn(45L);

        assertThatThrownBy(() -> otpService.generateAndSaveOtp("test@store.com"))
                .isInstanceOf(RateLimitException.class)
                .hasMessageContaining("45 giây");
    }

    @Test
    @DisplayName("generateAndSaveOtp should throw RateLimitException when hourly limit exceeded")
    void generateAndSaveOtp_hourlyLimitExceeded_throws() {
        when(redisTemplate.hasKey("auth:otp:cooldown:test@store.com")).thenReturn(false);
        when(valueOperations.increment("auth:otp:hourly:test@store.com")).thenReturn(6L);

        assertThatThrownBy(() -> otpService.generateAndSaveOtp("test@store.com"))
                .isInstanceOf(RateLimitException.class)
                .hasMessageContaining("quá nhiều lần trong 1 giờ");
    }

    @Test
    @DisplayName("verifyOtpAndGenerateResetToken should succeed and return UUID reset token")
    void verifyOtp_success() {
        when(valueOperations.get("auth:otp:test@store.com")).thenReturn("123456");

        String resetToken = otpService.verifyOtpAndGenerateResetToken("test@store.com", "123456");

        assertThat(resetToken).isNotNull().isNotBlank();
        verify(redisTemplate).delete("auth:otp:test@store.com");
        verify(valueOperations).set(eq("auth:reset-token:" + resetToken), eq("test@store.com"), eq(Duration.ofSeconds(600)));
    }

    @Test
    @DisplayName("verifyOtpAndGenerateResetToken should throw when OTP does not match")
    void verifyOtp_wrongOtp_throws() {
        when(valueOperations.get("auth:otp:test@store.com")).thenReturn("123456");
        when(valueOperations.increment("auth:otp:attempts:test@store.com")).thenReturn(1L);

        assertThatThrownBy(() -> otpService.verifyOtpAndGenerateResetToken("test@store.com", "999999"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Mã OTP không chính xác");
    }

    @Test
    @DisplayName("verifyOtpAndGenerateResetToken should lock OTP when attempts exceed 5")
    void verifyOtp_attemptsExceeded_throwsRateLimitException() {
        when(valueOperations.get("auth:otp:test@store.com")).thenReturn("123456");
        when(valueOperations.increment("auth:otp:attempts:test@store.com")).thenReturn(6L);

        assertThatThrownBy(() -> otpService.verifyOtpAndGenerateResetToken("test@store.com", "999999"))
                .isInstanceOf(RateLimitException.class)
                .hasMessageContaining("quá 5 lần");

        verify(redisTemplate).delete("auth:otp:test@store.com");
    }

    @Test
    @DisplayName("validateResetToken should return true when token matches stored email")
    void validateResetToken_valid() {
        when(valueOperations.get("auth:reset-token:my-token")).thenReturn("test@store.com");

        boolean isValid = otpService.validateResetToken("test@store.com", "my-token");

        assertThat(isValid).isTrue();
    }
}
