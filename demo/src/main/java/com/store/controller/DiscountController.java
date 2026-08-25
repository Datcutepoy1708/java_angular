package com.store.controller;

import com.store.dto.request.discount.ValidateDiscountRequest;
import com.store.dto.response.ApiResponse;
import com.store.dto.response.discount.DiscountResponse;
import com.store.dto.response.discount.DiscountValidationResult;
import com.store.security.CustomUserDetails;
import com.store.service.DiscountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/discounts")
@RequiredArgsConstructor
@Tag(name = "Discounts (Storefront)", description = "Public discount validation & discovery APIs for customers")
public class DiscountController {

    private final DiscountService discountService;

    @PostMapping("/validate")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Validate and preview discount calculation for current customer's cart")
    public ResponseEntity<ApiResponse<DiscountValidationResult>> validateDiscount(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ValidateDiscountRequest request) {
        DiscountValidationResult result = discountService.validateDiscountForCustomer(userDetails.getUserId(), request.getCode());
        return ResponseEntity.ok(ApiResponse.success(result.getMessage(), result));
    }

    @GetMapping("/public")
    @Operation(summary = "Get list of active public discount codes available for customers")
    public ResponseEntity<ApiResponse<List<DiscountResponse>>> getPublicDiscounts() {
        List<DiscountResponse> discounts = discountService.getPublicActiveDiscounts();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách mã giảm giá thành công", discounts));
    }
}
