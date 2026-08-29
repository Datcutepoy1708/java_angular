package com.store.service;

import com.store.dto.request.ChatBotRuleRequest;
import com.store.dto.response.ChatBotRuleResponse;

import java.util.List;

public interface ChatRuleService {

    List<ChatBotRuleResponse> getAllRules();

    ChatBotRuleResponse getRuleById(Integer id);

    ChatBotRuleResponse createRule(ChatBotRuleRequest request);

    ChatBotRuleResponse updateRule(Integer id, ChatBotRuleRequest request);

    void deleteRule(Integer id);

    /** Tải rules đang active theo priority giảm dần — có Redis cache. */
    List<ChatBotRuleResponse> getActiveRules();
}
