package com.store.dto.response;

import com.store.entity.chat.MessageSenderType;
import java.time.LocalDateTime;
import java.util.List;

public record ChatMessageDto(
        Long messageId,
        Long conversationId,
        MessageSenderType senderType,
        Long senderId,
        String senderName,
        String content,
        String attachmentUrl,
        List<String> quickReplies,
        boolean isRead,
        LocalDateTime createdAt
) {}
