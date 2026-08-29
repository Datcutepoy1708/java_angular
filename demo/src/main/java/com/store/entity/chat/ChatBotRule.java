package com.store.entity.chat;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_bot_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatBotRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rule_id")
    private Integer ruleId;

    @Column(name = "rule_name", nullable = false, length = 150)
    private String ruleName;

    /**
     * Comma-separated keywords, e.g. "bảo hành,warranty,lỗi"
     */
    @Column(name = "keywords", nullable = false, columnDefinition = "TEXT")
    private String keywords;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_type", nullable = false, columnDefinition = "enum('CONTAINS','EXACT','REGEX')")
    @Builder.Default
    private MatchType matchType = MatchType.CONTAINS;

    @Column(name = "response_message", nullable = false, columnDefinition = "TEXT")
    private String responseMessage;

    /**
     * JSON array of quick reply button labels, e.g. ["Bảo hành", "Đổi trả"]
     */
    @Column(name = "quick_replies", columnDefinition = "JSON")
    private String quickReplies;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, columnDefinition = "enum('REPLY','HANDOVER_STAFF')")
    @Builder.Default
    private RuleActionType actionType = RuleActionType.REPLY;

    @Column(name = "priority", nullable = false)
    @Builder.Default
    private int priority = 0;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
