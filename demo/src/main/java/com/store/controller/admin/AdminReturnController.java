package com.store.controller.admin;

import com.store.dto.response.ApiResponse;
import com.store.dto.returnrefund.*;
import com.store.security.CustomUserDetails;
import com.store.service.ReturnService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/returns")
@RequiredArgsConstructor
@Tag(name = "Admin Returns & Refunds", description = "APIs for managing return requests, inspection, and refund processing")
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
public class AdminReturnController {

    private final ReturnService returnService;

    @GetMapping
    @Operation(summary = "Get all return requests with dynamic filtering")
    public ResponseEntity<ApiResponse<Page<ReturnDetailResponse>>> getAdminReturnRequests(
            @ModelAttribute ReturnFilterRequest request
    ) {
        Page<ReturnDetailResponse> page = returnService.getAdminReturnRequests(request);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách yêu cầu đổi trả thành công", page));
    }

    @GetMapping("/metrics")
    @Operation(summary = "Get overview metrics for return & refund requests")
    public ResponseEntity<ApiResponse<ReturnMetricsResponse>> getReturnMetrics() {
        ReturnMetricsResponse metrics = returnService.getReturnMetrics();
        return ResponseEntity.ok(ApiResponse.success("Lấy thống kê đổi trả thành công", metrics));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get return request details by ID")
    public ResponseEntity<ApiResponse<ReturnDetailResponse>> getReturnRequestById(
            @PathVariable("id") Long returnId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        ReturnDetailResponse response = returnService.getReturnRequestById(returnId, currentUser.getUserId(), true);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết yêu cầu đổi trả thành công", response));
    }

    @PutMapping("/{id}/review")
    @Operation(summary = "Approve or reject a return request")
    public ResponseEntity<ApiResponse<ReturnDetailResponse>> reviewReturnRequest(
            @PathVariable("id") Long returnId,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody ReturnReviewRequest request
    ) {
        ReturnDetailResponse response = returnService.reviewReturnRequest(returnId, currentUser.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success("Phê duyệt yêu cầu đổi trả thành công", response));
    }

    @PutMapping("/{id}/receive")
    @Operation(summary = "Confirm receiving returned items at warehouse and update conditions")
    public ResponseEntity<ApiResponse<ReturnDetailResponse>> receiveReturnedItems(
            @PathVariable("id") Long returnId,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody ReturnReceiveItemRequest request
    ) {
        ReturnDetailResponse response = returnService.receiveReturnedItems(returnId, currentUser.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success("Tiếp nhận hàng hoàn tại kho thành công", response));
    }

    @PutMapping("/{id}/refund")
    @Operation(summary = "Process refund, atomic stock restoration, and discount decrement")
    public ResponseEntity<ApiResponse<ReturnDetailResponse>> processRefund(
            @PathVariable("id") Long returnId,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody ReturnProcessRefundRequest request
    ) {
        ReturnDetailResponse response = returnService.processRefund(returnId, currentUser.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success("Xử lý hoàn tiền và hoàn kho thành công", response));
    }
}
