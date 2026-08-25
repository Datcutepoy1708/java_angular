package com.store.controller;

import com.store.dto.request.review.CreateReviewRequest;
import com.store.dto.response.ApiResponse;
import com.store.dto.response.PageResponse;
import com.store.dto.response.review.RatingSummaryResponse;
import com.store.dto.response.review.ReviewResponse;
import com.store.security.CustomUserDetails;
import com.store.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/products/{productId}/reviews")
@RequiredArgsConstructor
@Tag(name = "Product Reviews", description = "Public & Customer Product Review APIs")
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/summary")
    @Operation(summary = "Get product rating breakdown and average score (Cached 15m)")
    public ResponseEntity<ApiResponse<RatingSummaryResponse>> getRatingSummary(
            @PathVariable Long productId
    ) {
        RatingSummaryResponse summary = reviewService.getProductRatingSummary(productId);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin đánh giá thành công", summary));
    }

    @GetMapping
    @Operation(summary = "Get paginated approved reviews for a product with optional star filter")
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> getProductReviews(
            @PathVariable Long productId,
            @RequestParam(required = false) Integer rating,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PageResponse<ReviewResponse> response = reviewService.getProductReviews(productId, rating, page, size);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách đánh giá thành công", response));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Submit or update product review (Upsert with Verified Purchase check)")
    public ResponseEntity<ApiResponse<ReviewResponse>> submitReview(
            @PathVariable Long productId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateReviewRequest request
    ) {
        ReviewResponse response = reviewService.submitReview(userDetails.getUserId(), productId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Gửi đánh giá sản phẩm thành công", response));
    }
}
