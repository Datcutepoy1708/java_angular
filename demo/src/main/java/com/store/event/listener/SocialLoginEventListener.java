package com.store.event.listener;

import com.store.audit.event.AuditLogEvent;
import com.store.event.SocialLoginPostProcessEvent;
import com.store.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class SocialLoginEventListener {

    private final StringRedisTemplate redisTemplate;
    private final EmailService emailService;
    private final ApplicationEventPublisher eventPublisher;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleSocialLoginPostProcess(SocialLoginPostProcessEvent event) {
        log.info("Processing post-login background tasks for user: {} (preHijack={}, newUser={})",
                event.getEmail(), event.isPreHijackSuspected(), event.isNewUser());

        boolean isSyntheticEmail = "ZALO".equalsIgnoreCase(event.getProvider())
                || (event.getEmail() != null && (event.getEmail().endsWith("@zalo.me")
                || event.getEmail().endsWith("@facebook.com")
                || event.getEmail().endsWith(".local")));

        try {
            if (event.isPreHijackSuspected()) {
                // 1. Ghi Redis để vô hiệu hóa access token cũ của kẻ tạo tài khoản trước đó (TTL 7 ngày)
                try {
                    redisTemplate.opsForValue().set(
                            "auth:token_valid_after:" + event.getUserId(),
                            String.valueOf(System.currentTimeMillis()),
                            7,
                            TimeUnit.DAYS
                    );
                    log.info("Set auth:token_valid_after in Redis for user {}", event.getUserId());
                } catch (Exception e) {
                    log.error("Failed to set token_valid_after in Redis: {}", e.getMessage());
                }

                // 2. Gửi email minh bạch, lịch sự (bỏ qua nếu là tài khoản định danh synthetic như Zalo)
                if (!isSyntheticEmail) {
                    String messageHtml = "<p>Chúng tôi phát hiện tài khoản với email này đã được khởi tạo trước đó nhưng chưa từng phát sinh hoạt động mua sắm hay xác thực.</p>"
                            + "<p>Khi bạn đăng nhập thành công qua <strong>" + event.getProvider() + "</strong>, hệ thống đã chính thức kích hoạt tài khoản và <strong>vô hiệu hóa các phiên đăng nhập cũ</strong> để bảo đảm bạn là người duy nhất nắm quyền kiểm soát tài khoản.</p>"
                            + "<p>Nếu bạn muốn đặt mật khẩu riêng để đăng nhập trực tiếp sau này, bạn có thể sử dụng chức năng <em>\"Quên mật khẩu\"</em> trên trang đăng nhập bất kỳ lúc nào.</p>";

                    emailService.sendSecurityAlert(
                            event.getEmail(),
                            event.getFullName(),
                            "[COMPLEXUS] Tài khoản của bạn đã được kích hoạt thành công qua " + event.getProvider(),
                            messageHtml
                    );
                } else {
                    log.info("Skipping security email for synthetic Zalo email: {}", event.getEmail());
                }

                // 3. Ghi Audit Log với cảnh báo bảo mật
                eventPublisher.publishEvent(AuditLogEvent.builder()
                        .userId(event.getUserId())
                        .userEmail(event.getEmail())
                        .module("AUTH")
                        .actionType("ACCOUNT_PRE_HIJACK_PREVENTED")
                        .recordId(String.valueOf(event.getUserId()))
                        .description("Phát hiện tài khoản chưa xác thực có dấu hiệu squatting. Đã thu hồi quyền truy cập cũ và cấp quyền sở hữu cho chủ thực sự qua "
                                + event.getProvider() + " (ID: " + event.getProviderId() + ")")
                        .ipAddress(event.getClientIp())
                        .userAgent(event.getUserAgent())
                        .status("WARNING")
                        .build());

            } else if (event.isNewUser()) {
                // Người dùng mới đăng ký lần đầu qua Google/Facebook
                eventPublisher.publishEvent(AuditLogEvent.builder()
                        .userId(event.getUserId())
                        .userEmail(event.getEmail())
                        .module("AUTH")
                        .actionType("USER_REGISTER_OAUTH")
                        .recordId(String.valueOf(event.getUserId()))
                        .description("Đăng ký tài khoản mới thành công qua nhà cung cấp " + event.getProvider() + " (ID: " + event.getProviderId() + ")")
                        .ipAddress(event.getClientIp())
                        .userAgent(event.getUserAgent())
                        .status("SUCCESS")
                        .build());

            } else {
                // Khách hàng thật đang hoạt động liên kết thêm nhà cung cấp xã hội
                String messageHtml = "<p>Tài khoản của bạn tại <strong>COMPLEXUS</strong> vừa được liên kết thành công với tài khoản <strong>"
                        + event.getProvider() + "</strong>.</p>"
                        + "<p>Giờ đây bạn có thể đăng nhập nhanh chóng bằng nút <strong>" + event.getProvider() + "</strong> hoặc tiếp tục sử dụng mật khẩu hiện tại bất kỳ lúc nào.</p>";

                if (!isSyntheticEmail) {
                    emailService.sendSecurityAlert(
                            event.getEmail(),
                            event.getFullName(),
                            "[COMPLEXUS] Liên kết tài khoản " + event.getProvider() + " thành công",
                            messageHtml
                    );
                } else {
                    log.info("Skipping account-linked security email for synthetic Zalo email: {}", event.getEmail());
                }

                eventPublisher.publishEvent(AuditLogEvent.builder()
                        .userId(event.getUserId())
                        .userEmail(event.getEmail())
                        .module("AUTH")
                        .actionType("ACCOUNT_LINKED_SOCIAL")
                        .recordId(String.valueOf(event.getUserId()))
                        .description("Tài khoản khách hàng liên kết thành công với nhà cung cấp " + event.getProvider() + " (ID: " + event.getProviderId() + ")")
                        .ipAddress(event.getClientIp())
                        .userAgent(event.getUserAgent())
                        .status("SUCCESS")
                        .build());
            }
        } catch (Exception e) {
            log.error("Error in handleSocialLoginPostProcess for email {}: {}", event.getEmail(), e.getMessage(), e);
        }
    }
}

    
    
        
    
