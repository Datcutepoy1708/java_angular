package com.store.controller;

import com.store.dto.request.discount.CreateDiscountRequest;
import com.store.dto.request.discount.DiscountFilterRequest;
import com.store.dto.request.discount.UpdateDiscountRequest;
import com.store.dto.response.ApiResponse;
import com.store.dto.response.PageResponse;
import com.store.dto.response.discount.DiscountMetricsResponse;
import com.store.dto.response.discount.DiscountResponse;
import com.store.dto.response.discount.DiscountUsageResponse;
import com.store.service.DiscountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/discount-codes")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
@Tag(name = "Admin Discounts", description = "Backoffice discount & coupon code management APIs")
public class AdminDiscountController {

    private final DiscountService discountService;

    @GetMapping
    @Operation(summary = "Get paginated list of discount codes with filters")
    public ResponseEntity<ApiResponse<PageResponse<DiscountResponse>>> getDiscounts(
            @ModelAttribute DiscountFilterRequest filter) {
        PageResponse<DiscountResponse> response = discountService.getAdminDiscounts(filter);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách mã giảm giá thành công", response));
    }

    @GetMapping("/metrics")
    @Operation(summary = "Get discount statistics & KPI metrics")
    public ResponseEntity<ApiResponse<DiscountMetricsResponse>> getMetrics() {
        DiscountMetricsResponse metrics = discountService.getMetrics();
        return ResponseEntity.ok(ApiResponse.success("Lấy thống kê mã giảm giá thành công", metrics));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get discount details by ID")
    public ResponseEntity<ApiResponse<DiscountResponse>> getDiscountById(@PathVariable Long id) {
        DiscountResponse response = discountService.getDiscountById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết mã giảm giá thành công", response));
    }

    @PostMapping
    @Operation(summary = "Create a new discount code")
    public ResponseEntity<ApiResponse<DiscountResponse>> createDiscount(
            @Valid @RequestBody CreateDiscountRequest request) {
        DiscountResponse response = discountService.createDiscount(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo mã giảm giá thành công", response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing discount code")
    public ResponseEntity<ApiResponse<DiscountResponse>> updateDiscount(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDiscountRequest request) {
        DiscountResponse response = discountService.updateDiscount(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật mã giảm giá thành công", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate/Soft-delete a discount code")
    public ResponseEntity<ApiResponse<Void>> deleteDiscount(@PathVariable Long id) {
        discountService.deleteDiscount(id);
        return ResponseEntity.ok(ApiResponse.success("Đã vô hiệu hóa mã giảm giá thành công", null));
    }

    @GetMapping("/{id}/usages")
    @Operation(summary = "Get customer usage history logs for a discount code")
    public ResponseEntity<ApiResponse<List<DiscountUsageResponse>>> getDiscountUsages(@PathVariable Long id) {
        List<DiscountUsageResponse> usages = discountService.getDiscountUsages(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy lịch sử sử dụng thành công", usages));
    }
}
