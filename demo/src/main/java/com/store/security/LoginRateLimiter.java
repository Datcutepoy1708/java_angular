package com.store.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoginRateLimiter {

    private final StringRedisTemplate redisTemplate;

    private static final String EMAIL_ATTEMPTS_PREFIX = "auth:ratelimit:login:email:";
    private static final String IP_ATTEMPTS_PREFIX = "auth:ratelimit:login:ip:";
    private static final int MAX_EMAIL_ATTEMPTS = 5;
    private static final int MAX_IP_ATTEMPTS = 20;
    private static final long LOCK_DURATION_MINUTES = 15;

    public void checkRateLimit(String email, String clientIp) {
        // 1. Kiểm tra giới hạn theo Email
        if (email != null && !email.isBlank()) {
            String emailKey = EMAIL_ATTEMPTS_PREFIX + email.trim().toLowerCase();
            checkKeyLimit(emailKey, MAX_EMAIL_ATTEMPTS, "Tài khoản bị tạm khóa do nhập sai mật khẩu quá " + MAX_EMAIL_ATTEMPTS + " lần.");
        }

        // 2. Kiểm tra giới hạn theo IP (chống tấn công phân tán nhiều tài khoản từ 1 IP)
        if (clientIp != null && !clientIp.isBlank()) {
            String ipKey = IP_ATTEMPTS_PREFIX + clientIp.trim();
            checkKeyLimit(ipKey, MAX_IP_ATTEMPTS, "Địa chỉ IP này bị tạm khóa do có quá nhiều lượt đăng nhập thất bại liên tiếp.");
        }
    }

    public void recordFailedAttempt(String email, String clientIp) {
        if (email != null && !email.isBlank()) {
            incrementWithExpiry(EMAIL_ATTEMPTS_PREFIX + email.trim().toLowerCase());
        }
        if (clientIp != null && !clientIp.isBlank()) {
            incrementWithExpiry(IP_ATTEMPTS_PREFIX + clientIp.trim());
        }
    }

    public void resetAttempts(String email, String clientIp) {
        if (email != null && !email.isBlank()) {
            redisTemplate.delete(EMAIL_ATTEMPTS_PREFIX + email.trim().toLowerCase());
        }
    }

    private void checkKeyLimit(String key, int maxAllowed, String errorMessage) {
        String attemptsStr = redisTemplate.opsForValue().get(key);
        if (attemptsStr != null) {
            try {
                int attempts = Integer.parseInt(attemptsStr);
                if (attempts >= maxAllowed) {
                    Long expireSeconds = redisTemplate.getExpire(key, TimeUnit.SECONDS);
                    long minutesLeft = (expireSeconds != null && expireSeconds > 0)
                            ? (expireSeconds / 60) + 1
                            : LOCK_DURATION_MINUTES;
                    log.warn("Rate limit triggered for key {}: {}/{} attempts. Time remaining: {} minutes",
                            key, attempts, maxAllowed, minutesLeft);
                    throw new IllegalArgumentException(errorMessage + " Vui lòng thử lại sau " + minutesLeft + " phút.");
                }
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private void incrementWithExpiry(String key) {
        Long attempts = redisTemplate.opsForValue().increment(key);
        if (attempts != null && attempts == 1) {
            redisTemplate.expire(key, LOCK_DURATION_MINUTES, TimeUnit.MINUTES);
        }
    }
}
