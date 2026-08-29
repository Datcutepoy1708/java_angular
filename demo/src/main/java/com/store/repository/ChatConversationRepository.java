package com.store.repository;

import com.store.entity.chat.ChatConversation;
import com.store.entity.chat.ConversationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ChatConversationRepository extends JpaRepository<ChatConversation, Long> {

    /**
     * Tìm hội thoại đang hoạt động của session (BOT_ACTIVE hoặc WAITING_STAFF).
     */
    Optional<ChatConversation> findFirstBySessionIdAndStatusInOrderByCreatedAtDesc(
            String sessionId, List<ConversationStatus> statuses);

    /**
     * Tìm hội thoại mới nhất theo sessionId (bao gồm cả CLOSED).
     */
    Optional<ChatConversation> findFirstBySessionIdOrderByCreatedAtDesc(String sessionId);

    /**
     * Lấy danh sách hội thoại theo trạng thái cho Admin queue.
     */
    Page<ChatConversation> findByStatusOrderByLastMessageAtDesc(ConversationStatus status, Pageable pageable);

    /**
     * Lấy tất cả hội thoại đang chờ (WAITING_STAFF) cho queue admin.
     */
    List<ChatConversation> findByStatusOrderByLastMessageAtAsc(ConversationStatus status);

    /**
     * Lấy hội thoại của một nhân viên cụ thể (STAFF_ACTIVE).
     */
    List<ChatConversation> findByStaffIdAndStatusOrderByLastMessageAtDesc(Long staffId, ConversationStatus status);

    // ─── Atomic Operations ───────────────────────────────────────────────────

    /**
     * Claim hội thoại nguyên tử — chống race condition khi nhiều staff nhận cùng lúc.
     * Chỉ cập nhật nếu hội thoại vẫn đang ở trạng thái WAITING_STAFF.
     */
    @Modifying
    @Query("UPDATE ChatConversation c SET c.staffId = :staffId, c.status = 'STAFF_ACTIVE' " +
           "WHERE c.conversationId = :id AND c.status = 'WAITING_STAFF'")
    int claimConversationAtomic(@Param("id") Long id, @Param("staffId") Long staffId);

    /**
     * Tăng unread cho nhân viên (khi khách gửi tin).
     */
    @Modifying
    @Query("UPDATE ChatConversation c SET c.unreadStaffCount = c.unreadStaffCount + 1, " +
           "c.lastMessage = :lastMsg, c.lastMessageAt = :now, c.updatedAt = :now " +
           "WHERE c.conversationId = :id")
    int incrementUnreadStaffCountAtomic(@Param("id") Long id,
                                        @Param("lastMsg") String lastMsg,
                                        @Param("now") LocalDateTime now);

    /**
     * Tăng unread cho khách hàng (khi nhân viên/bot gửi tin).
     */
    @Modifying
    @Query("UPDATE ChatConversation c SET c.unreadCustomerCount = c.unreadCustomerCount + 1, " +
           "c.lastMessage = :lastMsg, c.lastMessageAt = :now, c.updatedAt = :now " +
           "WHERE c.conversationId = :id")
    int incrementUnreadCustomerCountAtomic(@Param("id") Long id,
                                           @Param("lastMsg") String lastMsg,
                                           @Param("now") LocalDateTime now);

    /**
     * Reset unread staff về 0 (khi nhân viên mở xem hội thoại).
     */
    @Modifying
    @Query("UPDATE ChatConversation c SET c.unreadStaffCount = 0 WHERE c.conversationId = :id")
    int resetUnreadStaffCountAtomic(@Param("id") Long id);

    /**
     * Reset unread customer về 0 (khi khách mở widget chat).
     */
    @Modifying
    @Query("UPDATE ChatConversation c SET c.unreadCustomerCount = 0 WHERE c.conversationId = :id")
    int resetUnreadCustomerCountAtomic(@Param("id") Long id);

    /**
     * Tăng bot_unmatched_count nguyên tử.
     */
    @Modifying
    @Query("UPDATE ChatConversation c SET c.botUnmatchedCount = c.botUnmatchedCount + 1 " +
           "WHERE c.conversationId = :id")
    int incrementBotUnmatchedCountAtomic(@Param("id") Long id);

    /**
     * Reset bot_unmatched_count về 0 khi bot match thành công.
     */
    @Modifying
    @Query("UPDATE ChatConversation c SET c.botUnmatchedCount = 0 WHERE c.conversationId = :id")
    int resetBotUnmatchedCountAtomic(@Param("id") Long id);

    /**
     * Đổi trạng thái hội thoại.
     */
    @Modifying
    @Query("UPDATE ChatConversation c SET c.status = :status, c.updatedAt = :now " +
           "WHERE c.conversationId = :id")
    int updateStatusAtomic(@Param("id") Long id,
                           @Param("status") ConversationStatus status,
                           @Param("now") LocalDateTime now);

    /**
     * Hợp nhất phiên Guest khi đăng nhập: gắn userId, customerName, customerEmail.
     */
    @Modifying
    @Query("UPDATE ChatConversation c SET c.userId = :userId, c.customerName = :name, " +
           "c.customerEmail = :email, c.updatedAt = :now " +
           "WHERE c.sessionId = :sessionId AND c.userId IS NULL")
    int mergeGuestSession(@Param("sessionId") String sessionId,
                          @Param("userId") Long userId,
                          @Param("name") String name,
                          @Param("email") String email,
                          @Param("now") LocalDateTime now);
}
