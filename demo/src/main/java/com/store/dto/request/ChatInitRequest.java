package com.store.dto.request;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;

public record ChatInitRequest(
        @NotBlank(message = "Session ID không được để trống")
        @Length(max = 100)
        String sessionId,

        String customerName,
        String customerEmail,
        String customerPhone
) {}
