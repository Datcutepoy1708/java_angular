package com.store.dto.response;

/**
 * Typing signal gửi qua WebSocket để thông báo user đang gõ.
 * Dùng cho cả hướng khách→nhân viên và nhân viên→khách.
 */
public record ChatTypingSignal(
        Long conversationId,
        String senderType,  // "CUSTOMER" | "STAFF"
        String senderName,
        boolean isTyping
) {}
