package com.store.dto.request;

import com.store.entity.chat.MatchType;
import com.store.entity.chat.RuleActionType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

public record ChatBotRuleRequest(
        @NotBlank(message = "Tên rule không được để trống")
        @Length(max = 150)
        String ruleName,

        @NotBlank(message = "Từ khóa không được để trống")
        String keywords,

        @NotNull(message = "Match type không được null")
        MatchType matchType,

        @NotBlank(message = "Nội dung phản hồi không được để trống")
        String responseMessage,

        String quickReplies,

        @NotNull(message = "Action type không được null")
        RuleActionType actionType,

        @Min(value = 0, message = "Priority không được âm")
        int priority,

        boolean isActive
) {}
