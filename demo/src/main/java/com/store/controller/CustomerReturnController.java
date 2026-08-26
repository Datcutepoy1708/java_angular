package com.store.controller;

import com.store.dto.response.ApiResponse;
import com.store.dto.returnrefund.ReturnCreateRequest;
import com.store.dto.returnrefund.ReturnDetailResponse;
import com.store.security.CustomUserDetails;
import com.store.service.ReturnService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/returns")
@RequiredArgsConstructor
@Tag(name = "Customer Returns", description = "APIs for customers to create and track return & refund requests")
@PreAuthorize("isAuthenticated()")
public class CustomerReturnController {

    private final ReturnService returnService;

    @PostMapping
    @Operation(summary = "Submit a new return & refund request for completed order")
    public ResponseEntity<ApiResponse<ReturnDetailResponse>> createReturnRequest(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody ReturnCreateRequest request
    ) {
        ReturnDetailResponse response = returnService.createReturnRequest(currentUser.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Gửi yêu cầu đổi trả thành công", response));
    }

    @GetMapping
    @Operation(summary = "Get current customer's return requests")
    public ResponseEntity<ApiResponse<Page<ReturnDetailResponse>>> getMyReturnRequests(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        Page<ReturnDetailResponse> response = returnService.getCustomerReturnRequests(currentUser.getUserId(), page, size);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách yêu cầu đổi trả thành công", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get return request details by ID")
    public ResponseEntity<ApiResponse<ReturnDetailResponse>> getReturnRequestById(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable("id") Long returnId
    ) {
        ReturnDetailResponse response = returnService.getReturnRequestById(returnId, currentUser.getUserId(), false);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết yêu cầu đổi trả thành công", response));
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "Cancel a pending return request")
    public ResponseEntity<ApiResponse<ReturnDetailResponse>> cancelReturnRequest(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable("id") Long returnId
    ) {
        ReturnDetailResponse response = returnService.cancelReturnRequest(returnId, currentUser.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Đã hủy yêu cầu đổi trả thành công", response));
    }
}
