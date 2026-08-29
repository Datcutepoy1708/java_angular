package com.store.dto.response;

import com.store.entity.chat.ConversationStatus;
import java.time.LocalDateTime;

public record ChatConversationSummaryResponse(
        Long conversationId,
        String sessionId,
        Long userId,
        Long staffId,
        String staffName,
        String customerName,
        String customerEmail,
        String customerPhone,
        ConversationStatus status,
        int unreadStaffCount,
        int unreadCustomerCount,
        String lastMessage,
        LocalDateTime lastMessageAt,
        LocalDateTime createdAt
) {}
