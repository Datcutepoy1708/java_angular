package com.store.security;

import com.store.entity.order.OrderStatus;
import com.store.exception.RateLimitException;
import com.store.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class GuestOrderRateLimiter {

    private final StringRedisTemplate redisTemplate;
    private final OrderRepository orderRepository;

    private static final String IP_RATE_LIMIT_PREFIX = "order:ratelimit:guest:ip:";
    private static final String IP_BURST_PREFIX = "order:ratelimit:guest:burst:";

    private static final int MAX_ORDERS_PER_HOUR = 5;
    private static final long WINDOW_DURATION_MINUTES = 60;
    private static final long BURST_COOLDOWN_SECONDS = 30;
    private static final int MAX_PENDING_ORDERS_PER_PHONE = 5;

    /**
     * Check both IP rate limit (burst and hourly) and Receiver Phone pending orders count.
     */
    public void checkRateLimit(String clientIp, String receiverPhone) {
        // 1. Burst protection (giãn cách tối thiểu 30s giữa 2 request đặt hàng từ 1 IP)
        if (clientIp != null && !clientIp.isBlank()) {
            String burstKey = IP_BURST_PREFIX + clientIp.trim();
            String burstVal = redisTemplate.opsForValue().get(burstKey);
            if (burstVal != null) {
                Long expireSeconds = redisTemplate.getExpire(burstKey, TimeUnit.SECONDS);
                long secondsLeft = (expireSeconds != null && expireSeconds > 0) ? expireSeconds : BURST_COOLDOWN_SECONDS;
                log.warn("Guest order burst rate limit triggered for IP {}. Cooldown remaining: {}s", clientIp, secondsLeft);
                throw new RateLimitException("Thao tác quá nhanh. Quý khách vui lòng đợi " + secondsLeft + " giây trước khi thực hiện đặt đơn tiếp theo.");
            }

            // 2. IP hourly limit (tối đa 5 đơn/giờ/IP)
            String ipKey = IP_RATE_LIMIT_PREFIX + clientIp.trim();
            String attemptsStr = redisTemplate.opsForValue().get(ipKey);
            if (attemptsStr != null) {
                try {
                    int count = Integer.parseInt(attemptsStr);
                    if (count >= MAX_ORDERS_PER_HOUR) {
                        Long expireSeconds = redisTemplate.getExpire(ipKey, TimeUnit.SECONDS);
                        long minutesLeft = (expireSeconds != null && expireSeconds > 0) ? (expireSeconds / 60) + 1 : WINDOW_DURATION_MINUTES;
                        log.warn("Guest order hourly limit exceeded for IP {}: {}/{} orders. Time remaining: {}m", clientIp, count, MAX_ORDERS_PER_HOUR, minutesLeft);
                        throw new RateLimitException("Quý khách đã đạt giới hạn " + MAX_ORDERS_PER_HOUR + " đơn hàng vãng lai trong 1 giờ. Vui lòng đăng nhập tài khoản để tiếp tục đặt hàng không giới hạn hoặc thử lại sau " + minutesLeft + " phút.");
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }

        // 3. Receiver phone limit (tối đa 5 đơn PENDING trong 24 giờ cho cùng 1 SĐT)
        if (receiverPhone != null && !receiverPhone.isBlank()) {
            LocalDateTime since = LocalDateTime.now().minusHours(24);
            long pendingCount = orderRepository.countByReceiverPhoneAndOrderStatusAndCreatedAtAfter(
                    receiverPhone.trim(),
                    OrderStatus.PENDING,
                    since
            );
            if (pendingCount >= MAX_PENDING_ORDERS_PER_PHONE) {
                log.warn("Guest order phone limit exceeded for phone {}: {} pending orders in 24h", receiverPhone, pendingCount);
                throw new RateLimitException("Số điện thoại này hiện đang có " + pendingCount + " đơn hàng chờ xác nhận. Quý khách vui lòng đăng nhập tài khoản để tiếp tục đặt hàng hoặc liên hệ tổng đài để được hỗ trợ xác nhận đơn cũ.");
            }
        }
    }

    /**
     * Record a successful guest order attempt against the IP rate limiter.
     */
    public void recordOrder(String clientIp) {
        if (clientIp != null && !clientIp.isBlank()) {
            String ipKey = IP_RATE_LIMIT_PREFIX + clientIp.trim();
            Long count = redisTemplate.opsForValue().increment(ipKey);
            if (count != null && count == 1) {
                redisTemplate.expire(ipKey, WINDOW_DURATION_MINUTES, TimeUnit.MINUTES);
            }

            String burstKey = IP_BURST_PREFIX + clientIp.trim();
            redisTemplate.opsForValue().set(burstKey, "1", BURST_COOLDOWN_SECONDS, TimeUnit.SECONDS);
        }
    }
}
