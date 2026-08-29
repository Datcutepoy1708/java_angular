package com.store.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

public record ChatMessageSendRequest(
        @NotNull(message = "Conversation ID không được null")
        Long conversationId,

        @NotBlank(message = "Nội dung tin nhắn không được để trống")
        @Length(max = 5000, message = "Nội dung tin nhắn không được vượt quá 5000 ký tự")
        String content,

        String attachmentUrl
) {}
