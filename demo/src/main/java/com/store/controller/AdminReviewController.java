package com.store.controller;

import com.store.dto.request.review.ReviewFilterRequest;
import com.store.dto.response.ApiResponse;
import com.store.dto.response.PageResponse;
import com.store.dto.response.review.ReviewResponse;
import com.store.entity.review.ReviewStatus;
import com.store.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/reviews")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
@Tag(name = "Admin Reviews", description = "Admin Review Moderation APIs")
public class AdminReviewController {

    private final ReviewService reviewService;

    @GetMapping
    @Operation(summary = "Get paginated reviews for moderation with keyword & status filters")
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> getAdminReviews(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) ReviewStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        ReviewFilterRequest filter = ReviewFilterRequest.builder()
                .productId(productId)
                .rating(rating)
                .status(status)
                .keyword(keyword)
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sortDir(sortDir)
                .build();

        PageResponse<ReviewResponse> response = reviewService.getAdminReviews(filter);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách đánh giá quản trị thành công", response));
    }

    @PutMapping("/{reviewId}/status")
    @Operation(summary = "Update review status (APPROVED, PENDING, HIDDEN)")
    public ResponseEntity<ApiResponse<ReviewResponse>> updateReviewStatus(
            @PathVariable Long reviewId,
            @RequestParam ReviewStatus status
    ) {
        ReviewResponse response = reviewService.updateReviewStatus(reviewId, status);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái đánh giá thành công", response));
    }

    @DeleteMapping("/{reviewId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete inappropriate or spam review")
    public ResponseEntity<ApiResponse<Void>> deleteReview(@PathVariable Long reviewId) {
        reviewService.deleteReview(reviewId);
        return ResponseEntity.ok(ApiResponse.success("Xóa đánh giá thành công", null));
    }
}
