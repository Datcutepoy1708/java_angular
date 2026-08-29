package com.store.security;

import com.store.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.Principal;

/**
 * Intercept WebSocket STOMP CONNECT frame để xác thực JWT.
 * - Authenticated user: set Spring Security principal đầy đủ.
 * - Guest: set anonymous principal với sessionId làm định danh.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
            return message;
        }

        // Thử xác thực JWT từ header Authorization
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                if (jwtTokenProvider.validateToken(token)) {
                    String email = jwtTokenProvider.getEmailFromToken(token);
                    UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    accessor.setUser(auth);
                    log.debug("[WebSocket] Authenticated user connected: {}", email);
                }
            } catch (Exception e) {
                log.warn("[WebSocket] JWT validation failed on CONNECT: {}", e.getMessage());
                // Fail-open: kết nối vẫn được cho phép nhưng không có auth principal
            }
        } else {
            // Guest: dùng sessionId từ header X-Session-Id làm anonymous principal
            String sessionId = accessor.getFirstNativeHeader("X-Session-Id");
            if (StringUtils.hasText(sessionId)) {
                accessor.setUser(new GuestPrincipal(sessionId));
                log.debug("[WebSocket] Guest connected with sessionId: {}", sessionId);
            }
        }

        return message;
    }

    /**
     * Anonymous principal cho Guest users — định danh bằng sessionId.
     */
    public record GuestPrincipal(String sessionId) implements Principal {
        @Override
        public String getName() {
            return "guest:" + sessionId;
        }
    }
}
