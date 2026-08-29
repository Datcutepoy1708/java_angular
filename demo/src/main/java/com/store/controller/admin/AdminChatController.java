package com.store.controller.admin;

import com.store.dto.request.ChatBotRuleRequest;
import com.store.dto.request.ChatMessageSendRequest;
import com.store.dto.response.ApiResponse;
import com.store.dto.response.ChatBotRuleResponse;
import com.store.dto.response.ChatConversationSummaryResponse;
import com.store.dto.response.ChatMessageDto;
import com.store.entity.chat.ConversationStatus;
import com.store.security.CustomUserDetails;
import com.store.service.ChatRuleService;
import com.store.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/chat")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasAnyAuthority('CHAT_VIEW', 'CHAT_RESPOND', 'CHAT_MANAGE', 'CHAT_BOT_VIEW', 'CHAT_BOT_CREATE', 'CHAT_BOT_UPDATE', 'CHAT_BOT_DELETE', 'CHAT_BOT_MANAGE')")
@Tag(name = "Admin Chat", description = "Admin/Staff live chat management and bot rule CRUD APIs")
@SecurityRequirement(name = "bearerAuth")
public class AdminChatController {

    private final ChatService chatService;
    private final ChatRuleService chatRuleService;

    // ─── Conversation Queue ───────────────────────────────────────────────────

    @GetMapping("/conversations")
    @PreAuthorize("hasRole('ADMIN') or hasAnyAuthority('CHAT_VIEW', 'CHAT_MANAGE')")
    @Operation(summary = "Lấy danh sách hội thoại theo trạng thái (mặc định WAITING_STAFF)")
    public ResponseEntity<ApiResponse<List<ChatConversationSummaryResponse>>> getConversations(
            @RequestParam(defaultValue = "WAITING_STAFF") ConversationStatus status) {

        return ResponseEntity.ok(ApiResponse.success("Tải hội thoại thành công",
                chatService.getConversationsByStatus(status)));
    }

    @GetMapping("/conversations/my")
    @PreAuthorize("hasRole('ADMIN') or hasAnyAuthority('CHAT_VIEW', 'CHAT_RESPOND', 'CHAT_MANAGE')")
    @Operation(summary = "Lấy hội thoại đang xử lý của nhân viên hiện tại")
    public ResponseEntity<ApiResponse<List<ChatConversationSummaryResponse>>> getMyConversations(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        return ResponseEntity.ok(ApiResponse.success("Tải hội thoại của bạn thành công",
                chatService.getStaffConversations(userDetails.getUserId())));
    }

    @GetMapping("/conversations/{conversationId}/messages")
    @PreAuthorize("hasRole('ADMIN') or hasAnyAuthority('CHAT_VIEW', 'CHAT_MANAGE')")
    @Operation(summary = "Lấy tin nhắn của hội thoại (admin view)")
    public ResponseEntity<ApiResponse<List<ChatMessageDto>>> getMessages(
            @PathVariable Long conversationId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        // Mark read by staff when opened
        chatService.markReadByStaff(conversationId);
        // Reuse service — staff doesn't need sessionId validation
        return ResponseEntity.ok(ApiResponse.success("Tải tin nhắn thành công",
                chatService.getMessages(conversationId, null)));
    }

    @PostMapping("/conversations/{conversationId}/claim")
    @PreAuthorize("hasRole('ADMIN') or hasAnyAuthority('CHAT_RESPOND', 'CHAT_MANAGE')")
    @Operation(summary = "Tiếp nhận hội thoại (atomic, chống race condition)")
    public ResponseEntity<ApiResponse<ChatConversationSummaryResponse>> claimConversation(
            @PathVariable Long conversationId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        ChatConversationSummaryResponse result = chatService.claimConversation(
                conversationId, userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Đã tiếp nhận hội thoại thành công", result));
    }

    @PostMapping("/conversations/{conversationId}/close")
    @PreAuthorize("hasRole('ADMIN') or hasAnyAuthority('CHAT_RESPOND', 'CHAT_MANAGE')")
    @Operation(summary = "Kết thúc hội thoại")
    public ResponseEntity<ApiResponse<Void>> closeConversation(
            @PathVariable Long conversationId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        chatService.closeConversation(conversationId, userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Hội thoại đã được kết thúc", null));
    }

    @PatchMapping("/conversations/{conversationId}/mark-read")
    @PreAuthorize("hasRole('ADMIN') or hasAnyAuthority('CHAT_RESPOND', 'CHAT_VIEW', 'CHAT_MANAGE')")
    @Operation(summary = "Đánh dấu đã đọc (nhân viên mở hội thoại)")
    public ResponseEntity<ApiResponse<Void>> markReadByStaff(@PathVariable Long conversationId) {
        chatService.markReadByStaff(conversationId);
        return ResponseEntity.ok(ApiResponse.success("Đã đánh dấu đọc", null));
    }

    // ─── Bot Rules CRUD ───────────────────────────────────────────────────────

    @GetMapping("/bot-rules")
    @PreAuthorize("hasRole('ADMIN') or hasAnyAuthority('CHAT_BOT_VIEW', 'CHAT_BOT_MANAGE')")
    @Operation(summary = "Lấy tất cả bot rules")
    public ResponseEntity<ApiResponse<List<ChatBotRuleResponse>>> getAllRules() {
        return ResponseEntity.ok(ApiResponse.success("Tải bot rules thành công",
                chatRuleService.getAllRules()));
    }

    @GetMapping("/bot-rules/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAnyAuthority('CHAT_BOT_VIEW', 'CHAT_BOT_MANAGE')")
    @Operation(summary = "Lấy chi tiết một bot rule")
    public ResponseEntity<ApiResponse<ChatBotRuleResponse>> getRuleById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success("Tải bot rule thành công",
                chatRuleService.getRuleById(id)));
    }

    @PostMapping("/bot-rules")
    @PreAuthorize("hasRole('ADMIN') or hasAnyAuthority('CHAT_BOT_CREATE', 'CHAT_BOT_MANAGE')")
    @Operation(summary = "Tạo mới bot rule (Admin hoặc có quyền CHAT_BOT_CREATE)")
    public ResponseEntity<ApiResponse<ChatBotRuleResponse>> createRule(
            @Valid @RequestBody ChatBotRuleRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo bot rule thành công",
                        chatRuleService.createRule(request)));
    }

    @PutMapping("/bot-rules/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAnyAuthority('CHAT_BOT_UPDATE', 'CHAT_BOT_MANAGE')")
    @Operation(summary = "Cập nhật bot rule (Admin hoặc có quyền CHAT_BOT_UPDATE)")
    public ResponseEntity<ApiResponse<ChatBotRuleResponse>> updateRule(
            @PathVariable Integer id,
            @Valid @RequestBody ChatBotRuleRequest request) {

        return ResponseEntity.ok(ApiResponse.success("Cập nhật bot rule thành công",
                chatRuleService.updateRule(id, request)));
    }

    @DeleteMapping("/bot-rules/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAnyAuthority('CHAT_BOT_DELETE', 'CHAT_BOT_MANAGE')")
    @Operation(summary = "Xóa bot rule (Admin hoặc có quyền CHAT_BOT_DELETE)")
    public ResponseEntity<ApiResponse<Void>> deleteRule(@PathVariable Integer id) {
        chatRuleService.deleteRule(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa bot rule thành công", null));
    }
}
