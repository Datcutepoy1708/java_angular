package com.store.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ChatMergeSessionRequest(
        @NotBlank(message = "Session ID không được để trống")
        String sessionId
) {}
