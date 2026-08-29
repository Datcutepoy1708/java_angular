package com.store.repository;

import com.store.entity.chat.ChatBotRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatBotRuleRepository extends JpaRepository<ChatBotRule, Integer> {

    /**
     * Tải toàn bộ rules đang active, sắp xếp priority giảm dần (cao nhất xét trước).
     */
    List<ChatBotRule> findByIsActiveTrueOrderByPriorityDesc();
}
