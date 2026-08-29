package com.store.dto.response;

import com.store.entity.chat.MatchType;
import com.store.entity.chat.RuleActionType;
import java.time.LocalDateTime;
import java.util.List;

public record ChatBotRuleResponse(
        Integer ruleId,
        String ruleName,
        String keywords,
        MatchType matchType,
        String responseMessage,
        List<String> quickReplies,
        RuleActionType actionType,
        int priority,
        boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
