package com.store.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.store.dto.request.ChatBotRuleRequest;
import com.store.dto.response.ChatBotRuleResponse;
import com.store.entity.chat.ChatBotRule;
import com.store.exception.ResourceNotFoundException;
import com.store.repository.ChatBotRuleRepository;
import com.store.service.ChatRuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRuleServiceImpl implements ChatRuleService {

    private final ChatBotRuleRepository chatBotRuleRepository;
    private final ObjectMapper objectMapper;

    @Override
    public List<ChatBotRuleResponse> getAllRules() {
        return chatBotRuleRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public ChatBotRuleResponse getRuleById(Integer id) {
        ChatBotRule rule = chatBotRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bot rule không tồn tại với id: " + id));
        return toResponse(rule);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "chatBotRules", allEntries = true)
    public ChatBotRuleResponse createRule(ChatBotRuleRequest request) {
        ChatBotRule rule = ChatBotRule.builder()
                .ruleName(request.ruleName())
                .keywords(request.keywords())
                .matchType(request.matchType())
                .responseMessage(request.responseMessage())
                .quickReplies(request.quickReplies())
                .actionType(request.actionType())
                .priority(request.priority())
                .isActive(request.isActive())
                .build();
        return toResponse(chatBotRuleRepository.save(rule));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "chatBotRules", allEntries = true)
    public ChatBotRuleResponse updateRule(Integer id, ChatBotRuleRequest request) {
        ChatBotRule rule = chatBotRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bot rule không tồn tại với id: " + id));
        rule.setRuleName(request.ruleName());
        rule.setKeywords(request.keywords());
        rule.setMatchType(request.matchType());
        rule.setResponseMessage(request.responseMessage());
        rule.setQuickReplies(request.quickReplies());
        rule.setActionType(request.actionType());
        rule.setPriority(request.priority());
        rule.setActive(request.isActive());
        return toResponse(chatBotRuleRepository.save(rule));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "chatBotRules", allEntries = true)
    public void deleteRule(Integer id) {
        if (!chatBotRuleRepository.existsById(id)) {
            throw new ResourceNotFoundException("Bot rule không tồn tại với id: " + id);
        }
        chatBotRuleRepository.deleteById(id);
    }

    @Override
    @Cacheable(cacheNames = "chatBotRules")
    public List<ChatBotRuleResponse> getActiveRules() {
        return chatBotRuleRepository.findByIsActiveTrueOrderByPriorityDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    // ─── Mapper ──────────────────────────────────────────────────────────────

    private ChatBotRuleResponse toResponse(ChatBotRule rule) {
        List<String> quickReplies = parseQuickReplies(rule.getQuickReplies());
        return new ChatBotRuleResponse(
                rule.getRuleId(),
                rule.getRuleName(),
                rule.getKeywords(),
                rule.getMatchType(),
                rule.getResponseMessage(),
                quickReplies,
                rule.getActionType(),
                rule.getPriority(),
                rule.isActive(),
                rule.getCreatedAt(),
                rule.getUpdatedAt()
        );
    }

    private List<String> parseQuickReplies(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("[ChatRuleService] Failed to parse quick_replies JSON: {}", json);
            return List.of();
        }
    }
}
