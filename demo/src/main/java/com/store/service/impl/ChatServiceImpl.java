package com.store.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.store.dto.request.ChatInitRequest;
import com.store.dto.request.ChatMergeSessionRequest;
import com.store.dto.request.ChatMessageSendRequest;
import com.store.dto.response.ChatBotRuleResponse;
import com.store.dto.response.ChatConversationSummaryResponse;
import com.store.dto.response.ChatInitResponse;
import com.store.dto.response.ChatMessageDto;
import com.store.entity.chat.*;
import com.store.exception.BadRequestException;
import com.store.exception.ConflictException;
import com.store.exception.ResourceNotFoundException;
import com.store.repository.ChatConversationRepository;
import com.store.repository.ChatMessageRepository;
import com.store.service.ChatService;
import com.store.service.ChatUploadRateLimiter;
import com.store.util.FileSecurityValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatConversationRepository conversationRepo;
    private final ChatMessageRepository messageRepo;
    private final ChatBotEngineServiceImpl botEngine;
    private final ChatUploadRateLimiter uploadRateLimiter;
    private final FileSecurityValidator fileSecurityValidator;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    // ─── 1. Init Conversation ─────────────────────────────────────────────────

    @Override
    @Transactional
    public ChatInitResponse initConversation(ChatInitRequest request, Long userId) {
        // Tìm hội thoại đang hoạt động của session
        var existing = conversationRepo.findFirstBySessionIdAndStatusInOrderByCreatedAtDesc(
                request.sessionId(),
                List.of(ConversationStatus.BOT_ACTIVE, ConversationStatus.WAITING_STAFF, ConversationStatus.STAFF_ACTIVE)
        );

        if (existing.isPresent()) {
            ChatConversation conv = existing.get();
            // Reset unread customer count khi khách mở widget
            conversationRepo.resetUnreadCustomerCountAtomic(conv.getConversationId());
            return new ChatInitResponse(
                    conv.getConversationId(),
                    conv.getSessionId(),
                    conv.getStatus(),
                    0,
                    conv.getCreatedAt()
            );
        }

        // Tạo mới hội thoại
        ChatConversation newConv = ChatConversation.builder()
                .sessionId(request.sessionId())
                .userId(userId)
                .customerName(request.customerName())
                .customerEmail(request.customerEmail())
                .customerPhone(request.customerPhone())
                .status(ConversationStatus.BOT_ACTIVE)
                .build();
        newConv = conversationRepo.save(newConv);

        // Gửi tin chào mừng tự động từ bot
        sendBotWelcomeMessage(newConv.getConversationId());

        return new ChatInitResponse(
                newConv.getConversationId(),
                newConv.getSessionId(),
                newConv.getStatus(),
                0,
                newConv.getCreatedAt()
        );
    }

    // ─── 2. Get Messages ─────────────────────────────────────────────────────

    @Override
    public List<ChatMessageDto> getMessages(Long conversationId, String sessionId) {
        ChatConversation conv = getConversationByIdOrThrow(conversationId);
        // sessionId = null means admin/staff access — no session validation needed
        if (sessionId != null && !conv.getSessionId().equals(sessionId)) {
            throw new BadRequestException("Bạn không có quyền xem hội thoại này");
        }
        // Reset unread customer (khi khách gọi)
        if (sessionId != null) {
            conversationRepo.resetUnreadCustomerCountAtomic(conversationId);
        }
        return messageRepo.findByConversationIdOrderByCreatedAtAsc(conversationId)
                .stream()
                .map(this::toMessageDto)
                .toList();
    }

    // ─── 3. Customer Sends Message ────────────────────────────────────────────

    @Override
    @Transactional
    public ChatMessageDto sendCustomerMessage(ChatMessageSendRequest request, String sessionId) {
        ChatConversation conv = getConversationByIdOrThrow(request.conversationId());

        if (!conv.getSessionId().equals(sessionId)) {
            throw new BadRequestException("Session không hợp lệ cho hội thoại này");
        }
        if (conv.getStatus() == ConversationStatus.CLOSED) {
            throw new BadRequestException("Hội thoại đã đóng, không thể gửi tin nhắn");
        }

        // Lưu tin nhắn khách hàng
        ChatMessage customerMsg = saveMessage(
                conv.getConversationId(),
                MessageSenderType.CUSTOMER,
                null,
                conv.getCustomerName() != null ? conv.getCustomerName() : "Khách",
                request.content(),
                request.attachmentUrl(),
                null
        );

        // Tăng unread staff atomic
        conversationRepo.incrementUnreadStaffCountAtomic(
                conv.getConversationId(),
                truncateLastMessage(request.content()),
                LocalDateTime.now()
        );

        ChatMessageDto customerMsgDto = toMessageDto(customerMsg);

        // Broadcast tin nhắn khách tới nhân viên
        broadcastToStaff(conv.getConversationId(), customerMsgDto);

        // Xử lý bot nếu hội thoại đang BOT_ACTIVE
        if (conv.getStatus() == ConversationStatus.BOT_ACTIVE) {
            processBotResponse(conv, request.content());
        }

        return customerMsgDto;
    }

    // ─── 4. Staff Sends Message ───────────────────────────────────────────────

    @Override
    @Transactional
    public ChatMessageDto sendStaffMessage(ChatMessageSendRequest request, Long staffId, String staffName) {
        ChatConversation conv = getConversationByIdOrThrow(request.conversationId());

        if (conv.getStatus() == ConversationStatus.CLOSED) {
            throw new BadRequestException("Hội thoại đã đóng, không thể gửi tin nhắn");
        }

        ChatMessage staffMsg = saveMessage(
                conv.getConversationId(),
                MessageSenderType.STAFF,
                staffId,
                staffName,
                request.content(),
                request.attachmentUrl(),
                null
        );

        // Tăng unread customer atomic
        conversationRepo.incrementUnreadCustomerCountAtomic(
                conv.getConversationId(),
                truncateLastMessage(request.content()),
                LocalDateTime.now()
        );

        ChatMessageDto staffMsgDto = toMessageDto(staffMsg);

        // Broadcast tới khách hàng
        broadcastToCustomer(conv.getConversationId(), staffMsgDto);

        // Broadcast tới admin queue để cập nhật last_message
        broadcastQueueUpdate(conv.getConversationId());

        return staffMsgDto;
    }

    // ─── 5. Claim Conversation (Atomic) ──────────────────────────────────────

    @Override
    @Transactional
    public ChatConversationSummaryResponse claimConversation(Long conversationId, Long staffId) {
        int rows = conversationRepo.claimConversationAtomic(conversationId, staffId);
        if (rows == 0) {
            throw new ConflictException("Hội thoại đã được nhân viên khác tiếp nhận hoặc đã kết thúc");
        }

        // Reset unread staff
        conversationRepo.resetUnreadStaffCountAtomic(conversationId);

        ChatConversation conv = getConversationByIdOrThrow(conversationId);

        // Broadcast tới tất cả admin dashboard
        broadcastQueueUpdate(conversationId);

        log.info("[Chat] Staff {} claimed conversation {}", staffId, conversationId);
        return toSummaryResponse(conv);
    }

    // ─── 6. Close Conversation ────────────────────────────────────────────────

    @Override
    @Transactional
    public void closeConversation(Long conversationId, Long closedByStaffId) {
        ChatConversation conv = getConversationByIdOrThrow(conversationId);
        if (conv.getStatus() == ConversationStatus.CLOSED) return;

        conversationRepo.updateStatusAtomic(conversationId, ConversationStatus.CLOSED, LocalDateTime.now());

        // Gửi tin nhắn SYSTEM thông báo đóng
        saveMessage(conversationId, MessageSenderType.SYSTEM, null, "Hệ thống",
                "Hội thoại đã được kết thúc. Cảm ơn bạn đã liên hệ Complexus!", null, null);

        broadcastToCustomer(conversationId, null); // Signal reload
        broadcastQueueUpdate(conversationId);
        log.info("[Chat] Conversation {} closed by staff {}", conversationId, closedByStaffId);
    }

    // ─── 7. Queue & Staff Queries ─────────────────────────────────────────────

    @Override
    public List<ChatConversationSummaryResponse> getConversationsByStatus(ConversationStatus status) {
        return conversationRepo.findByStatusOrderByLastMessageAtAsc(status)
                .stream().map(this::toSummaryResponse).toList();
    }

    @Override
    public List<ChatConversationSummaryResponse> getStaffConversations(Long staffId) {
        return conversationRepo.findByStaffIdAndStatusOrderByLastMessageAtDesc(staffId, ConversationStatus.STAFF_ACTIVE)
                .stream().map(this::toSummaryResponse).toList();
    }

    // ─── 8. Mark Read ─────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void markReadByStaff(Long conversationId) {
        conversationRepo.resetUnreadStaffCountAtomic(conversationId);
        messageRepo.markAllAsRead(conversationId);
    }

    @Override
    @Transactional
    public void markReadByCustomer(Long conversationId, String sessionId) {
        ChatConversation conv = getConversationByIdOrThrow(conversationId);
        if (!conv.getSessionId().equals(sessionId)) return;
        conversationRepo.resetUnreadCustomerCountAtomic(conversationId);
    }

    // ─── 9. Merge Guest Session ───────────────────────────────────────────────

    @Override
    @Transactional
    public void mergeGuestSession(ChatMergeSessionRequest request, Long userId, String userName, String userEmail) {
        int updated = conversationRepo.mergeGuestSession(
                request.sessionId(), userId, userName, userEmail, LocalDateTime.now()
        );
        log.info("[Chat] Merged {} conversation(s) for session {} → userId {}", updated, request.sessionId(), userId);
    }

    // ─── 10. Upload Image ─────────────────────────────────────────────────────

    @Override
    public String uploadChatImage(MultipartFile file, String ipAddress, String sessionId) {
        // Rate limiting: 5 ảnh / phút / IP
        uploadRateLimiter.checkAndIncrement(ipAddress);

        // Security validation (magic bytes, size, extension)
        fileSecurityValidator.validateImageFile(file);

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        int extIndex = originalFilename != null ? originalFilename.lastIndexOf('.') : -1;
        if (extIndex >= 0) {
            extension = originalFilename.substring(extIndex).toLowerCase(Locale.ROOT);
        }

        try {
            Path uploadDir = Paths.get("uploads", "chat").toAbsolutePath().normalize();
            if (!Files.exists(uploadDir)) Files.createDirectories(uploadDir);

            String newFilename = UUID.randomUUID() + extension;
            Path targetLocation = uploadDir.resolve(newFilename).normalize();

            // Anti-path traversal
            if (!targetLocation.startsWith(uploadDir)) {
                throw new BadRequestException("Đường dẫn lưu file không an toàn");
            }

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
            }

            return ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/uploads/chat/")
                    .path(newFilename)
                    .toUriString();
        } catch (IOException ex) {
            log.error("[Chat] Failed to save chat image", ex);
            throw new BadRequestException("Không thể lưu ảnh. Vui lòng thử lại");
        }
    }

    // ─── Bot Engine ───────────────────────────────────────────────────────────

    private void processBotResponse(ChatConversation conv, String customerMessage) {
        Long convId = conv.getConversationId();

        // Kiểm tra chuyển giao chủ động
        if (botEngine.isExplicitHandoverRequest(customerMessage)) {
            conversationRepo.updateStatusAtomic(convId, ConversationStatus.WAITING_STAFF, LocalDateTime.now());
            conversationRepo.resetBotUnmatchedCountAtomic(convId);

            ChatMessage handoverMsg = saveMessage(convId, MessageSenderType.BOT, null, "Complexus Bot",
                    botEngine.buildExplicitHandoverResponse(), null, null);
            broadcastToCustomer(convId, toMessageDto(handoverMsg));
            broadcastAdminQueue(convId);
            log.info("[Chat] Explicit handover requested for conversation {}", convId);
            return;
        }

        // Tìm matching rule
        ChatBotRuleResponse matchedRule = botEngine.findMatchingRule(customerMessage);

        if (matchedRule != null) {
            // Match thành công — reset unmatched count
            conversationRepo.resetBotUnmatchedCountAtomic(convId);

            String metadata = null;
            List<String> quickReplies = matchedRule.quickReplies();
            if (quickReplies != null && !quickReplies.isEmpty()) {
                metadata = toJson(quickReplies);
            }

            ChatMessage botMsg = saveMessage(convId, MessageSenderType.BOT, null, "Complexus Bot",
                    matchedRule.responseMessage(), null, metadata);
            ChatMessageDto botMsgDto = toMessageDto(botMsg);
            // Set quick replies vào DTO khi broadcast
            broadcastToCustomer(convId, toMessageDtoWithQuickReplies(botMsg, quickReplies));

            // Nếu rule là HANDOVER_STAFF → chuyển trạng thái luôn
            if (matchedRule.actionType() == com.store.entity.chat.RuleActionType.HANDOVER_STAFF) {
                conversationRepo.updateStatusAtomic(convId, ConversationStatus.WAITING_STAFF, LocalDateTime.now());
                broadcastAdminQueue(convId);
            }
        } else {
            // Không khớp → tăng bộ đếm
            conversationRepo.incrementBotUnmatchedCountAtomic(convId);
            // Reload để lấy count mới (đã increment trong DB)
            ChatConversation refreshed = getConversationByIdOrThrow(convId);
            int unmatchedCount = refreshed.getBotUnmatchedCount();

            if (botEngine.shouldEscalate(unmatchedCount)) {
                // Lần 2 liên tiếp → tự escalate
                conversationRepo.updateStatusAtomic(convId, ConversationStatus.WAITING_STAFF, LocalDateTime.now());
                ChatMessage escalateMsg = saveMessage(convId, MessageSenderType.BOT, null, "Complexus Bot",
                        botEngine.buildEscalationResponse(), null, null);
                broadcastToCustomer(convId, toMessageDto(escalateMsg));
                broadcastAdminQueue(convId);
                log.info("[Chat] Auto-escalated conversation {} to WAITING_STAFF (unmatched={})", convId, unmatchedCount);
            } else {
                // Lần 1 → gợi ý thân thiện
                List<String> quickReplies = botEngine.buildDefaultQuickReplies();
                ChatMessage helpMsg = saveMessage(convId, MessageSenderType.BOT, null, "Complexus Bot",
                        botEngine.buildFirstUnmatchedResponse(), null, toJson(quickReplies));
                broadcastToCustomer(convId, toMessageDtoWithQuickReplies(helpMsg, quickReplies));
            }
        }

        // Tăng unread customer (bot gửi tin)
        conversationRepo.incrementUnreadCustomerCountAtomic(convId, "", LocalDateTime.now());
    }

    private void sendBotWelcomeMessage(Long convId) {
        List<String> quickReplies = List.of("Tư vấn sản phẩm", "Kiểm tra bảo hành", "Chính sách đổi trả", "Báo giá", "Gặp nhân viên tư vấn");
        String welcome = "Xin chào! Tôi là trợ lý ảo của Complexus. Tôi có thể giúp bạn tư vấn về sản phẩm, bảo hành, đổi trả và nhiều hơn nữa. Bạn cần hỗ trợ gì hôm nay?";
        saveMessage(convId, MessageSenderType.BOT, null, "Complexus Bot", welcome, null, toJson(quickReplies));
    }

    // ─── WebSocket Broadcasts ─────────────────────────────────────────────────

    /** Broadcast tin nhắn tới khách hàng đang xem conversation */
    private void broadcastToCustomer(Long conversationId, ChatMessageDto msg) {
        messagingTemplate.convertAndSend("/topic/chat/" + conversationId, msg != null ? msg : "CLOSED");
    }

    /** Broadcast tin nhắn tới nhân viên đang xem conversation */
    private void broadcastToStaff(Long conversationId, ChatMessageDto msg) {
        messagingTemplate.convertAndSend("/topic/admin/chat/" + conversationId, msg);
    }

    /** Broadcast queue update tới tất cả admin dashboard */
    private void broadcastQueueUpdate(Long conversationId) {
        messagingTemplate.convertAndSend("/topic/admin/chat-queue", conversationId);
    }

    /** Broadcast khi hội thoại mới vào hàng đợi chờ nhân viên */
    private void broadcastAdminQueue(Long conversationId) {
        messagingTemplate.convertAndSend("/topic/admin/chat-queue", conversationId);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private ChatConversation getConversationByIdOrThrow(Long id) {
        return conversationRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hội thoại không tồn tại: " + id));
    }

    private ChatMessage saveMessage(Long conversationId, MessageSenderType senderType,
                                    Long senderId, String senderName,
                                    String content, String attachmentUrl, String metadata) {
        ChatMessage msg = ChatMessage.builder()
                .conversationId(conversationId)
                .senderType(senderType)
                .senderId(senderId)
                .senderName(senderName)
                .content(content)
                .attachmentUrl(attachmentUrl)
                .metadata(metadata)
                .build();
        return messageRepo.save(msg);
    }

    private ChatMessageDto toMessageDto(ChatMessage msg) {
        return toMessageDtoWithQuickReplies(msg, parseQuickReplies(msg.getMetadata()));
    }

    private ChatMessageDto toMessageDtoWithQuickReplies(ChatMessage msg, List<String> quickReplies) {
        return new ChatMessageDto(
                msg.getMessageId(),
                msg.getConversationId(),
                msg.getSenderType(),
                msg.getSenderId(),
                msg.getSenderName(),
                msg.getContent(),
                msg.getAttachmentUrl(),
                quickReplies,
                msg.isRead(),
                msg.getCreatedAt()
        );
    }

    private ChatConversationSummaryResponse toSummaryResponse(ChatConversation conv) {
        return new ChatConversationSummaryResponse(
                conv.getConversationId(),
                conv.getSessionId(),
                conv.getUserId(),
                conv.getStaffId(),
                null, // staffName resolved by frontend or separate query if needed
                conv.getCustomerName(),
                conv.getCustomerEmail(),
                conv.getCustomerPhone(),
                conv.getStatus(),
                conv.getUnreadStaffCount(),
                conv.getUnreadCustomerCount(),
                conv.getLastMessage(),
                conv.getLastMessageAt(),
                conv.getCreatedAt()
        );
    }

    private String truncateLastMessage(String content) {
        if (content == null) return "";
        return content.length() > 100 ? content.substring(0, 100) + "..." : content;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return null;
        }
    }

    private List<String> parseQuickReplies(String metadata) {
        if (metadata == null || metadata.isBlank()) return List.of();
        try {
            return objectMapper.readValue(metadata,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (Exception e) {
            return List.of();
        }
    }
}
