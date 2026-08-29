package com.store.dto.response;

import com.store.entity.chat.ConversationStatus;
import java.time.LocalDateTime;

public record ChatInitResponse(
        Long conversationId,
        String sessionId,
        ConversationStatus status,
        int unreadCustomerCount,
        LocalDateTime createdAt
) {}
