package com.store.controller;

import com.store.dto.request.ChatMessageSendRequest;
import com.store.dto.response.ChatMessageDto;
import com.store.dto.response.ChatTypingSignal;
import com.store.security.CustomUserDetails;
import com.store.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * Xử lý WebSocket STOMP messages.
 * Routes: /app/chat.send, /app/chat.typing
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Xử lý tin nhắn gửi từ client (khách hoặc nhân viên).
     * Client gửi tới: /app/chat.send
     * Backend broadcast tới: /topic/chat/{conversationId} và /topic/admin/chat/{conversationId}
     */
    @MessageMapping("/chat.send")
    public void handleSendMessage(@Payload ChatMessageSendRequest request, Principal principal) {
        if (principal == null) {
            log.warn("[ChatWS] Unauthenticated message send attempt rejected");
            return;
        }

        try {
            if (isStaffPrincipal(principal)) {
                // Nhân viên gửi tin
                CustomUserDetails userDetails = (CustomUserDetails) ((org.springframework.security.authentication.UsernamePasswordAuthenticationToken) principal).getPrincipal();
                chatService.sendStaffMessage(request, userDetails.getUserId(), userDetails.getFullName());
            } else {
                // Khách hàng (Guest hoặc Authenticated customer) gửi tin
                String sessionId = extractSessionId(principal);
                chatService.sendCustomerMessage(request, sessionId);
            }
        } catch (Exception e) {
            log.error("[ChatWS] Error handling chat.send for conversation {}: {}",
                    request.conversationId(), e.getMessage(), e);
        }
    }

    /**
     * Xử lý typing signal.
     * Client gửi tới: /app/chat.typing
     * Backend broadcast tới: /topic/admin/chat/{conversationId} hoặc /topic/chat/{conversationId}
     */
    @MessageMapping("/chat.typing")
    public void handleTyping(@Payload ChatTypingSignal signal, Principal principal) {
        if (principal == null) return;

        try {
            if (isStaffPrincipal(principal)) {
                // Nhân viên đang gõ → broadcast tới khách
                messagingTemplate.convertAndSend(
                        "/topic/chat/" + signal.conversationId() + "/typing", signal);
            } else {
                // Khách đang gõ → broadcast tới nhân viên
                messagingTemplate.convertAndSend(
                        "/topic/admin/chat/" + signal.conversationId() + "/typing", signal);
            }
        } catch (Exception e) {
            log.warn("[ChatWS] Error handling typing signal: {}", e.getMessage());
        }
    }

    // ─── Private Helpers ─────────────────────────────────────────────────────

    private boolean isStaffPrincipal(Principal principal) {
        if (principal instanceof org.springframework.security.authentication.UsernamePasswordAuthenticationToken auth) {
            return auth.getPrincipal() instanceof CustomUserDetails userDetails
                    && userDetails.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_STAFF"));
        }
        return false;
    }

    private String extractSessionId(Principal principal) {
        // GuestPrincipal: "guest:{sessionId}"
        if (principal instanceof com.store.security.WebSocketAuthChannelInterceptor.GuestPrincipal guestPrincipal) {
            return guestPrincipal.sessionId();
        }
        // Authenticated customer: dùng username (email) làm fallback
        return principal.getName();
    }
}
