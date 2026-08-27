package com.store.service.impl;

import com.store.exception.RateLimitException;
import com.store.service.OtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {

    private final StringRedisTemplate redisTemplate;
    private final SecureRandom secureRandom = new SecureRandom();

    private static final String OTP_PREFIX = "auth:otp:";
    private static final String COOLDOWN_PREFIX = "auth:otp:cooldown:";
    private static final String HOURLY_LIMIT_PREFIX = "auth:otp:hourly:";
    private static final String ATTEMPTS_PREFIX = "auth:otp:attempts:";
    private static final String RESET_TOKEN_PREFIX = "auth:reset-token:";

    private static final long OTP_TTL_SECONDS = 300;         // 5 minutes
    private static final long COOLDOWN_SECONDS = 60;          // 1 minute
    private static final long HOURLY_LIMIT_SECONDS = 3600;    // 1 hour
    private static final long RESET_TOKEN_TTL_SECONDS = 600;  // 10 minutes
    private static final int MAX_HOURLY_REQUESTS = 5;
    private static final int MAX_FAILED_ATTEMPTS = 5;

    @Override
    public String generateAndSaveOtp(String email) {
        String normalizedEmail = email.trim().toLowerCase();

        // 1. Check 60s cooldown
        String cooldownKey = COOLDOWN_PREFIX + normalizedEmail;
        Boolean hasCooldown = redisTemplate.hasKey(cooldownKey);
        if (Boolean.TRUE.equals(hasCooldown)) {
            Long ttl = redisTemplate.getExpire(cooldownKey);
            long secondsLeft = (ttl != null && ttl > 0) ? ttl : COOLDOWN_SECONDS;
            throw new RateLimitException("Vui lòng đợi " + secondsLeft + " giây trước khi yêu cầu gửi lại mã OTP.");
        }

        // 2. Check hourly rate limit (max 5 requests/hour)
        String hourlyKey = HOURLY_LIMIT_PREFIX + normalizedEmail;
        Long hourlyCount = redisTemplate.opsForValue().increment(hourlyKey);
        if (hourlyCount != null && hourlyCount == 1) {
            redisTemplate.expire(hourlyKey, Duration.ofSeconds(HOURLY_LIMIT_SECONDS));
        } else if (hourlyCount != null && hourlyCount > MAX_HOURLY_REQUESTS) {
            throw new RateLimitException("Bạn đã yêu cầu gửi mã OTP quá nhiều lần trong 1 giờ. Vui lòng thử lại sau.");
        }

        // 3. Generate 6-digit OTP (e.g. 042819)
        String otp = String.format("%06d", secureRandom.nextInt(1_000_000));

        // 4. Save OTP to Redis with 5-minute TTL
        String otpKey = OTP_PREFIX + normalizedEmail;
        redisTemplate.opsForValue().set(otpKey, otp, Duration.ofSeconds(OTP_TTL_SECONDS));

        // 5. Set 60-second cooldown
        redisTemplate.opsForValue().set(cooldownKey, "1", Duration.ofSeconds(COOLDOWN_SECONDS));

        // 6. Reset failed attempts counter
        redisTemplate.delete(ATTEMPTS_PREFIX + normalizedEmail);

        log.info("Generated OTP for email: {} (TTL: {}s)", normalizedEmail, OTP_TTL_SECONDS);
        return otp;
    }

    @Override
    public String verifyOtpAndGenerateResetToken(String email, String otp) {
        String normalizedEmail = email.trim().toLowerCase();
        String otpKey = OTP_PREFIX + normalizedEmail;
        String storedOtp = redisTemplate.opsForValue().get(otpKey);

        if (storedOtp == null) {
            throw new IllegalArgumentException("Mã OTP không tồn tại hoặc đã hết hạn. Vui lòng yêu cầu mã mới.");
        }

        // Check failed attempts
        String attemptsKey = ATTEMPTS_PREFIX + normalizedEmail;
        Long attempts = redisTemplate.opsForValue().increment(attemptsKey);
        if (attempts != null && attempts == 1) {
            redisTemplate.expire(attemptsKey, Duration.ofSeconds(OTP_TTL_SECONDS));
        }

        if (attempts != null && attempts > MAX_FAILED_ATTEMPTS) {
            redisTemplate.delete(otpKey);
            redisTemplate.delete(attemptsKey);
            throw new RateLimitException("Bạn đã nhập sai mã OTP quá 5 lần. Mã xác thực đã bị hủy để bảo mật. Vui lòng yêu cầu mã mới.");
        }

        if (!storedOtp.equals(otp.trim())) {
            int remaining = MAX_FAILED_ATTEMPTS - (attempts != null ? attempts.intValue() : 1);
            throw new IllegalArgumentException("Mã OTP không chính xác. Bạn còn " + Math.max(0, remaining) + " lần thử.");
        }

        // OTP verified successfully: clean up OTP & attempts
        redisTemplate.delete(otpKey);
        redisTemplate.delete(attemptsKey);

        // Generate one-time reset token (UUID)
        String resetToken = UUID.randomUUID().toString();
        String resetTokenKey = RESET_TOKEN_PREFIX + resetToken;
        redisTemplate.opsForValue().set(resetTokenKey, normalizedEmail, Duration.ofSeconds(RESET_TOKEN_TTL_SECONDS));

        log.info("Verified OTP successfully for email: {}. Generated reset token.", normalizedEmail);
        return resetToken;
    }

    @Override
    public boolean validateResetToken(String email, String resetToken) {
        if (resetToken == null || resetToken.isBlank()) {
            return false;
        }
        String resetTokenKey = RESET_TOKEN_PREFIX + resetToken.trim();
        String storedEmail = redisTemplate.opsForValue().get(resetTokenKey);
        return storedEmail != null && storedEmail.equalsIgnoreCase(email.trim());
    }

    @Override
    public void clearResetToken(String email, String resetToken) {
        if (resetToken != null && !resetToken.isBlank()) {
            String resetTokenKey = RESET_TOKEN_PREFIX + resetToken.trim();
            redisTemplate.delete(resetTokenKey);
        }
    }
}
