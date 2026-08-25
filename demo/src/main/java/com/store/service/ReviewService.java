package com.store.service;

import com.store.dto.request.review.CreateReviewRequest;
import com.store.dto.request.review.ReviewFilterRequest;
import com.store.dto.response.PageResponse;
import com.store.dto.response.review.RatingSummaryResponse;
import com.store.dto.response.review.ReviewResponse;
import com.store.entity.review.ReviewStatus;

public interface ReviewService {

    ReviewResponse submitReview(Long userId, Long productId, CreateReviewRequest request);

    RatingSummaryResponse getProductRatingSummary(Long productId);

    PageResponse<ReviewResponse> getProductReviews(Long productId, Integer ratingFilter, int page, int size);

    PageResponse<ReviewResponse> getAdminReviews(ReviewFilterRequest filter);

    ReviewResponse updateReviewStatus(Long reviewId, ReviewStatus status);

    void deleteReview(Long reviewId);
}
