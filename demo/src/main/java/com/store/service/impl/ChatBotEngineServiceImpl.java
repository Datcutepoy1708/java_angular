package com.store.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.store.dto.response.ChatBotRuleResponse;
import com.store.entity.chat.ChatBotRule;
import com.store.entity.chat.MatchType;
import com.store.entity.chat.RuleActionType;
import com.store.service.ChatRuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Bot engine logic: keyword matching theo priority và escalation logic.
 * Tách riêng khỏi ChatServiceImpl để dễ test độc lập.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatBotEngineServiceImpl {

    private static final int ESCALATE_THRESHOLD = 2;

    // Từ khóa chuyển giao chủ động — khách gõ → chuyển ngay WAITING_STAFF
    private static final List<String> HANDOVER_KEYWORDS = List.of(
            "gặp nhân viên", "gap nhan vien",
            "tư vấn viên", "tu van vien",
            "gặp người thật", "gap nguoi that",
            "hỗ trợ trực tiếp", "ho tro truc tiep",
            "kết nối nhân viên", "ket noi nhan vien",
            "gặp người", "gap nguoi",
            "nhân viên", "nhan vien"
    );

    private final ChatRuleService chatRuleService;
    private final ObjectMapper objectMapper;

    /**
     * Kiểm tra xem tin nhắn có phải là yêu cầu chuyển giao chủ động không.
     */
    public boolean isExplicitHandoverRequest(String message) {
        String normalized = normalizeVietnamese(message.toLowerCase().trim());
        return HANDOVER_KEYWORDS.stream()
                .anyMatch(kw -> normalized.contains(normalizeVietnamese(kw)));
    }

    /**
     * Tìm rule phù hợp nhất với tin nhắn.
     * Trả về null nếu không khớp rule nào.
     */
    public ChatBotRuleResponse findMatchingRule(String message) {
        List<ChatBotRuleResponse> activeRules = chatRuleService.getActiveRules();
        String normalizedMessage = normalizeVietnamese(message.toLowerCase().trim());

        for (ChatBotRuleResponse rule : activeRules) {
            if (matches(normalizedMessage, rule)) {
                return rule;
            }
        }
        return null;
    }

    /**
     * Tạo nội dung phản hồi cho lần không khớp thứ 1.
     */
    public String buildFirstUnmatchedResponse() {
        return "Xin lỗi, tôi chưa hiểu rõ câu hỏi của bạn. Bạn có thể chọn các chủ đề gợi ý bên dưới hoặc bấm \"Gặp nhân viên tư vấn\" để được hỗ trợ ngay.";
    }

    /**
     * Tạo nội dung phản hồi khi bot tự động chuyển sang nhân viên (lần 2 unmatched).
     */
    public String buildEscalationResponse() {
        return "Dạ, câu hỏi của bạn cần nhân viên chuyên môn tư vấn chi tiết. Tôi đang chuyển kết nối bạn tới nhân viên hỗ trợ trực tuyến, vui lòng chờ trong giây lát...";
    }

    /**
     * Tạo nội dung phản hồi khi khách chủ động yêu cầu gặp nhân viên.
     */
    public String buildExplicitHandoverResponse() {
        return "Đang kết nối bạn với nhân viên tư vấn của Complexus. Vui lòng chờ trong giây lát, nhân viên sẽ tiếp nhận ngay!";
    }

    /**
     * Quick replies mặc định cho lần unmatched đầu tiên.
     */
    public List<String> buildDefaultQuickReplies() {
        return List.of("Bảo hành", "Đổi trả", "Báo giá", "Giao hàng", "Gặp nhân viên tư vấn");
    }

    public boolean shouldEscalate(int botUnmatchedCount) {
        return botUnmatchedCount >= ESCALATE_THRESHOLD;
    }

    // ─── Private Helpers ─────────────────────────────────────────────────────

    private boolean matches(String normalizedMessage, ChatBotRuleResponse rule) {
        String[] keywords = rule.keywords().split(",");
        return switch (rule.matchType()) {
            case CONTAINS -> {
                for (String kw : keywords) {
                    String normalizedKw = normalizeVietnamese(kw.trim().toLowerCase());
                    if (!normalizedKw.isEmpty() && normalizedMessage.contains(normalizedKw)) {
                        yield true;
                    }
                }
                yield false;
            }
            case EXACT -> {
                for (String kw : keywords) {
                    String normalizedKw = normalizeVietnamese(kw.trim().toLowerCase());
                    if (normalizedMessage.equals(normalizedKw)) {
                        yield true;
                    }
                }
                yield false;
            }
            case REGEX -> {
                for (String kw : keywords) {
                    try {
                        if (normalizedMessage.matches("(?i).*" + kw.trim() + ".*")) {
                            yield true;
                        }
                    } catch (Exception e) {
                        log.warn("[ChatBotEngine] Invalid regex pattern in rule {}: {}", rule.ruleId(), kw);
                    }
                }
                yield false;
            }
        };
    }

    /**
     * Chuẩn hóa tiếng Việt: bỏ dấu để so sánh không phân biệt dấu.
     * VD: "bảo hành" → "bao hanh"
     */
    private String normalizeVietnamese(String input) {
        if (input == null) return "";
        String normalized = java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                         .replace("đ", "d").replace("Đ", "D");
    }

    /**
     * Parse JSON quick_replies string → List<String>.
     */
    public List<String> parseQuickReplies(String quickRepliesJson) {
        if (quickRepliesJson == null || quickRepliesJson.isBlank()) return List.of();
        try {
            return objectMapper.readValue(quickRepliesJson, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("[ChatBotEngine] Failed to parse quick_replies JSON: {}", quickRepliesJson);
            return List.of();
        }
    }
}
