package com.store.service;

import com.store.dto.request.ChatInitRequest;
import com.store.dto.request.ChatMergeSessionRequest;
import com.store.dto.request.ChatMessageSendRequest;
import com.store.dto.response.ChatConversationSummaryResponse;
import com.store.dto.response.ChatInitResponse;
import com.store.dto.response.ChatMessageDto;
import com.store.entity.chat.ConversationStatus;

import java.util.List;

public interface ChatService {

    /** Khởi tạo hoặc tiếp tục hội thoại của một session. */
    ChatInitResponse initConversation(ChatInitRequest request, Long userId);

    /** Lấy danh sách tin nhắn của hội thoại. */
    List<ChatMessageDto> getMessages(Long conversationId, String sessionId);

    /**
     * Xử lý tin nhắn từ khách hàng:
     * - Lưu message
     * - Gọi bot engine nếu status=BOT_ACTIVE
     * - Tăng unread_staff_count atomic
     * - Broadcast WebSocket
     */
    ChatMessageDto sendCustomerMessage(ChatMessageSendRequest request, String sessionId);

    /**
     * Xử lý tin nhắn từ nhân viên:
     * - Lưu message
     * - Tăng unread_customer_count atomic
     * - Broadcast WebSocket tới khách
     */
    ChatMessageDto sendStaffMessage(ChatMessageSendRequest request, Long staffId, String staffName);

    /**
     * Claim hội thoại nguyên tử — chống race condition.
     * Ném ConflictException nếu đã có staff khác claim.
     */
    ChatConversationSummaryResponse claimConversation(Long conversationId, Long staffId);

    /** Đóng hội thoại. */
    void closeConversation(Long conversationId, Long closedByStaffId);

    /** Lấy danh sách hội thoại theo status cho admin. */
    List<ChatConversationSummaryResponse> getConversationsByStatus(ConversationStatus status);

    /** Lấy danh sách hội thoại của một nhân viên. */
    List<ChatConversationSummaryResponse> getStaffConversations(Long staffId);

    /** Reset unread staff count khi nhân viên mở xem. */
    void markReadByStaff(Long conversationId);

    /** Reset unread customer count khi khách mở widget. */
    void markReadByCustomer(Long conversationId, String sessionId);

    /** Hợp nhất phiên Guest khi đăng nhập thành công. */
    void mergeGuestSession(ChatMergeSessionRequest request, Long userId, String userName, String userEmail);

    /** Upload ảnh chat với Redis rate limiting. */
    String uploadChatImage(org.springframework.web.multipart.MultipartFile file, String ipAddress, String sessionId);
}
