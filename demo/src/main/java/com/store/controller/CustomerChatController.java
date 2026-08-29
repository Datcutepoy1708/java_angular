package com.store.controller;

import com.store.dto.request.ChatInitRequest;
import com.store.dto.request.ChatMergeSessionRequest;
import com.store.dto.response.ApiResponse;
import com.store.dto.response.ChatInitResponse;
import com.store.dto.response.ChatMessageDto;
import com.store.security.CustomUserDetails;
import com.store.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@Tag(name = "Customer Chat", description = "Customer-facing live chat APIs (no auth required for guest)")
public class CustomerChatController {

    private final ChatService chatService;

    @PostMapping("/init")
    @Operation(summary = "Khởi tạo hoặc tiếp tục hội thoại chat")
    public ResponseEntity<ApiResponse<ChatInitResponse>> initConversation(
            @Valid @RequestBody ChatInitRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userId = userDetails != null ? userDetails.getUserId() : null;
        ChatInitResponse response = chatService.initConversation(request, userId);
        return ResponseEntity.ok(ApiResponse.success("Hội thoại đã được khởi tạo", response));
    }

    @GetMapping("/{conversationId}/messages")
    @Operation(summary = "Lấy lịch sử tin nhắn của hội thoại")
    public ResponseEntity<ApiResponse<List<ChatMessageDto>>> getMessages(
            @PathVariable Long conversationId,
            @RequestHeader("X-Session-Id") String sessionId) {

        List<ChatMessageDto> messages = chatService.getMessages(conversationId, sessionId);
        return ResponseEntity.ok(ApiResponse.success("Tải tin nhắn thành công", messages));
    }

    @PostMapping(value = "/upload-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload ảnh trong chat (giới hạn 5 ảnh/phút/IP)")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadChatImage(
            @RequestParam("file") MultipartFile file,
            @RequestHeader("X-Session-Id") String sessionId,
            HttpServletRequest httpRequest) {

        String ipAddress = getClientIp(httpRequest);
        String imageUrl = chatService.uploadChatImage(file, ipAddress, sessionId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tải ảnh lên thành công", Map.of("url", imageUrl)));
    }

    @PostMapping("/merge-session")
    @Operation(summary = "Hợp nhất phiên Guest vào tài khoản sau khi đăng nhập")
    public ResponseEntity<ApiResponse<Void>> mergeSession(
            @Valid @RequestBody ChatMergeSessionRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Cần đăng nhập để hợp nhất phiên"));
        }
        chatService.mergeGuestSession(request, userDetails.getUserId(),
                userDetails.getFullName(), userDetails.getEmail());
        return ResponseEntity.ok(ApiResponse.success("Hợp nhất phiên thành công", null));
    }

    @PatchMapping("/{conversationId}/mark-read")
    @Operation(summary = "Đánh dấu đã đọc (khách mở widget)")
    public ResponseEntity<ApiResponse<Void>> markReadByCustomer(
            @PathVariable Long conversationId,
            @RequestHeader("X-Session-Id") String sessionId) {

        chatService.markReadByCustomer(conversationId, sessionId);
        return ResponseEntity.ok(ApiResponse.success("Đã đánh dấu đọc", null));
    }

    // ─── Utilities ────────────────────────────────────────────────────────────

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}
