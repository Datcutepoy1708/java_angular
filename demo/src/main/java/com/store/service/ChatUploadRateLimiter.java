package com.store.service;

import com.store.exception.RateLimitException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis-based rate limiter cho việc upload ảnh trong chat.
 * Giới hạn tối đa 5 lần upload / phút / IP.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatUploadRateLimiter {

    private static final int MAX_UPLOADS_PER_MINUTE = 5;
    private static final Duration TTL = Duration.ofMinutes(1);
    private static final String KEY_PREFIX = "chat:upload:ratelimit:";

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * Kiểm tra và tăng bộ đếm upload cho IP.
     * Ném RateLimitException nếu vượt quá ngưỡng cho phép.
     *
     * @param ip Địa chỉ IP của client
     */
    public void checkAndIncrement(String ip) {
        String key = KEY_PREFIX + ip;
        Long count = stringRedisTemplate.opsForValue().increment(key);

        if (count == null) {
            log.warn("[ChatUploadRateLimiter] Redis increment returned null for key: {}", key);
            return; // Fail-open: cho phép upload khi Redis gặp sự cố
        }

        // Lần đầu tiên tạo key → đặt TTL
        if (count == 1) {
            stringRedisTemplate.expire(key, TTL);
        }

        if (count > MAX_UPLOADS_PER_MINUTE) {
            log.warn("[ChatUploadRateLimiter] Rate limit exceeded for IP: {}, count: {}", ip, count);
            throw new RateLimitException("Bạn đã tải lên quá nhiều ảnh, vui lòng thử lại sau 1 phút.");
        }
    }
}
