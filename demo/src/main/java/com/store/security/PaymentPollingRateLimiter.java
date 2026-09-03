package com.store.security;

import com.store.exception.RateLimitException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentPollingRateLimiter {

    private final StringRedisTemplate redisTemplate;

    private static final String IP_POLL_PREFIX = "payment:ratelimit:polling:ip:";
    private static final String TOKEN_POLL_PREFIX = "payment:ratelimit:polling:token:";

    private static final int MAX_REQUESTS_PER_MINUTE_IP = 40;
    private static final int MAX_REQUESTS_PER_MINUTE_TOKEN = 20;

    public void checkRateLimit(String clientIp, String tokenHash) {
        if (clientIp != null && !clientIp.isBlank()) {
            checkLimit(IP_POLL_PREFIX + clientIp.trim(), MAX_REQUESTS_PER_MINUTE_IP, "Too many payment status requests from this IP. Please wait a minute.");
        }

        if (tokenHash != null && !tokenHash.isBlank()) {
            checkLimit(TOKEN_POLL_PREFIX + tokenHash.trim(), MAX_REQUESTS_PER_MINUTE_TOKEN, "Too many status checks for this payment session. Please slow down.");
        }
    }

    private void checkLimit(String key, int maxRequests, String errorMessage) {
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1) {
                redisTemplate.expire(key, Duration.ofMinutes(1));
            }
            if (count != null && count > maxRequests) {
                log.warn("Rate limit exceeded for key {}: {}/{}", key, count, maxRequests);
                throw new RateLimitException(errorMessage);
            }
        } catch (RateLimitException rle) {
            throw rle;
        } catch (Exception e) {
            log.warn("Unable to check redis rate limit for key {}: {}", key, e.getMessage());
        }
    }
}
