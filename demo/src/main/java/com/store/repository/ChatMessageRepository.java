package com.store.repository;

import com.store.entity.chat.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /**
     * Lấy toàn bộ tin nhắn của một hội thoại, sắp xếp theo thời gian tăng dần.
     */
    List<ChatMessage> findByConversationIdOrderByCreatedAtAsc(Long conversationId);

    /**
     * Lấy N tin nhắn mới nhất (cho pagination load more).
     */
    @Query("SELECT m FROM ChatMessage m WHERE m.conversationId = :conversationId " +
           "ORDER BY m.createdAt DESC LIMIT :limit")
    List<ChatMessage> findLatestMessages(@Param("conversationId") Long conversationId,
                                         @Param("limit") int limit);

    /**
     * Đánh dấu tất cả tin nhắn của hội thoại là đã đọc.
     */
    @Modifying
    @Query("UPDATE ChatMessage m SET m.isRead = true WHERE m.conversationId = :conversationId AND m.isRead = false")
    int markAllAsRead(@Param("conversationId") Long conversationId);
}
