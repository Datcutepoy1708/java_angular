package com.store.dto.response.review;

import com.store.entity.review.Review;
import com.store.entity.review.ReviewStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewResponse {

    private Long reviewId;
    private Long productId;
    private String productName;
    private Long userId;
    private String userFullName;
    private String userEmail;
    private Integer rating;
    private String title;
    private String comment;
    private Boolean isVerifiedPurchase;
    private ReviewStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ReviewResponse fromEntity(Review review) {
        if (review == null) {
            return null;
        }
        return ReviewResponse.builder()
                .reviewId(review.getReviewId())
                .productId(review.getProduct() != null ? review.getProduct().getProductId() : null)
                .productName(review.getProduct() != null ? review.getProduct().getName() : null)
                .userId(review.getUser() != null ? review.getUser().getUserId() : null)
                .userFullName(review.getUser() != null ? review.getUser().getFullName() : null)
                .userEmail(review.getUser() != null ? review.getUser().getEmail() : null)
                .rating(review.getRating())
                .title(review.getTitle())
                .comment(review.getComment())
                .isVerifiedPurchase(review.getIsVerifiedPurchase())
                .status(review.getStatus())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}
