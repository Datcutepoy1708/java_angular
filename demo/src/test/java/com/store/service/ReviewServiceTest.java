package com.store.service;

import com.store.dto.request.review.CreateReviewRequest;
import com.store.dto.response.review.RatingSummaryResponse;
import com.store.dto.response.review.ReviewResponse;
import com.store.entity.product.Product;
import com.store.entity.review.Review;
import com.store.entity.review.ReviewStatus;
import com.store.entity.user.User;
import com.store.repository.ProductRepository;
import com.store.repository.ReviewRepository;
import com.store.repository.UserRepository;
import com.store.service.impl.ReviewServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private User testUser;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .userId(1L)
                .email("customer@example.com")
                .fullName("Nguyen Van A")
                .build();

        testProduct = Product.builder()
                .productId(100L)
                .name("Laptop Gaming ASUS ROG")
                .build();
    }

    @Test
    @DisplayName("Submit review creates new review with verified purchase when user has completed order")
    void testSubmitReview_NewReview_VerifiedPurchase() {
        CreateReviewRequest request = CreateReviewRequest.builder()
                .rating(5)
                .title("Sản phẩm rất tốt")
                .comment("Máy chạy mượt, build chắc chắn!")
                .build();

        when(productRepository.findById(100L)).thenReturn(Optional.of(testProduct));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(reviewRepository.hasUserPurchasedProduct(1L, 100L)).thenReturn(true);
        when(reviewRepository.findByUser_UserIdAndProduct_ProductId(1L, 100L)).thenReturn(Optional.empty());

        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
            Review r = invocation.getArgument(0);
            r.setReviewId(10L);
            return r;
        });

        ReviewResponse response = reviewService.submitReview(1L, 100L, request);

        assertThat(response).isNotNull();
        assertThat(response.getReviewId()).isEqualTo(10L);
        assertThat(response.getRating()).isEqualTo(5);
        assertThat(response.getTitle()).isEqualTo("Sản phẩm rất tốt");
        assertThat(response.getIsVerifiedPurchase()).isTrue();
        assertThat(response.getStatus()).isEqualTo(ReviewStatus.APPROVED);
        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    @DisplayName("Submit review performs upsert (updates existing review) when user already reviewed product")
    void testSubmitReview_ExistingReview_Upsert() {
        CreateReviewRequest request = CreateReviewRequest.builder()
                .rating(4)
                .title("Cập nhật lại nhận xét")
                .comment("Sau 1 tháng dùng vẫn ổn")
                .build();

        Review existingReview = Review.builder()
                .reviewId(20L)
                .product(testProduct)
                .user(testUser)
                .rating(5)
                .title("Đánh giá ban đầu")
                .comment("Mới mua thấy ổn")
                .isVerifiedPurchase(true)
                .status(ReviewStatus.APPROVED)
                .build();

        when(productRepository.findById(100L)).thenReturn(Optional.of(testProduct));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(reviewRepository.hasUserPurchasedProduct(1L, 100L)).thenReturn(true);
        when(reviewRepository.findByUser_UserIdAndProduct_ProductId(1L, 100L)).thenReturn(Optional.of(existingReview));
        when(reviewRepository.save(any(Review.class))).thenReturn(existingReview);

        ReviewResponse response = reviewService.submitReview(1L, 100L, request);

        assertThat(response).isNotNull();
        assertThat(response.getReviewId()).isEqualTo(20L);
        assertThat(response.getRating()).isEqualTo(4);
        assertThat(response.getTitle()).isEqualTo("Cập nhật lại nhận xét");
        assertThat(response.getComment()).isEqualTo("Sau 1 tháng dùng vẫn ổn");
        verify(reviewRepository).save(existingReview);
    }

    @Test
    @DisplayName("Submit review sets isVerifiedPurchase = false when user has not bought product")
    void testSubmitReview_NotPurchased() {
        CreateReviewRequest request = CreateReviewRequest.builder()
                .rating(3)
                .title("Hỏi về sản phẩm")
                .comment("Chưa mua nhưng thấy thông số ổn")
                .build();

        when(productRepository.findById(100L)).thenReturn(Optional.of(testProduct));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(reviewRepository.hasUserPurchasedProduct(1L, 100L)).thenReturn(false);
        when(reviewRepository.findByUser_UserIdAndProduct_ProductId(1L, 100L)).thenReturn(Optional.empty());

        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
            Review r = invocation.getArgument(0);
            r.setReviewId(30L);
            return r;
        });

        ReviewResponse response = reviewService.submitReview(1L, 100L, request);

        assertThat(response.getIsVerifiedPurchase()).isFalse();
    }

    @Test
    @DisplayName("Rating summary calculates accurately from single aggregate GROUP BY query (0 N+1)")
    void testGetProductRatingSummary_SingleQueryAggregation() {
        // Mock query returning [rating, count]
        List<Object[]> queryResult = new ArrayList<>();
        queryResult.add(new Object[]{5, 10L}); // 10 reviews of 5 stars (50)
        queryResult.add(new Object[]{4, 5L});  // 5 reviews of 4 stars (20)
        // Total reviews = 15, Total score = 70, Avg = 70/15 = 4.666... -> rounded 4.7

        when(reviewRepository.countReviewsGroupByRating(100L)).thenReturn(queryResult);

        RatingSummaryResponse summary = reviewService.getProductRatingSummary(100L);

        assertThat(summary).isNotNull();
        assertThat(summary.getProductId()).isEqualTo(100L);
        assertThat(summary.getTotalReviews()).isEqualTo(15L);
        assertThat(summary.getAverageRating()).isEqualTo(4.7);
        assertThat(summary.getRatingCounts().get(5)).isEqualTo(10L);
        assertThat(summary.getRatingCounts().get(4)).isEqualTo(5L);
        assertThat(summary.getRatingCounts().get(1)).isEqualTo(0L);
        assertThat(summary.getStarPercentages().get(5)).isEqualTo(66.7); // 10/15 * 100 = 66.666... -> 66.7
        assertThat(summary.getStarPercentages().get(4)).isEqualTo(33.3); // 5/15 * 100 = 33.333... -> 33.3
        assertThat(summary.getStarPercentages().get(1)).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Rating summary returns zero values when product has no reviews")
    void testGetProductRatingSummary_Empty() {
        when(reviewRepository.countReviewsGroupByRating(100L)).thenReturn(List.of());

        RatingSummaryResponse summary = reviewService.getProductRatingSummary(100L);

        assertThat(summary).isNotNull();
        assertThat(summary.getTotalReviews()).isEqualTo(0L);
        assertThat(summary.getAverageRating()).isEqualTo(0.0);
        assertThat(summary.getRatingCounts().get(5)).isEqualTo(0L);
        assertThat(summary.getStarPercentages().get(5)).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Update review status updates status successfully")
    void testUpdateReviewStatus_Success() {
        Review review = Review.builder()
                .reviewId(50L)
                .status(ReviewStatus.PENDING)
                .build();

        when(reviewRepository.findById(50L)).thenReturn(Optional.of(review));
        when(reviewRepository.save(any(Review.class))).thenReturn(review);

        ReviewResponse response = reviewService.updateReviewStatus(50L, ReviewStatus.APPROVED);

        assertThat(response.getStatus()).isEqualTo(ReviewStatus.APPROVED);
        verify(reviewRepository).save(review);
    }

    @Test
    @DisplayName("Delete review deletes successfully")
    void testDeleteReview_Success() {
        when(reviewRepository.existsById(50L)).thenReturn(true);

        reviewService.deleteReview(50L);

        verify(reviewRepository).deleteById(50L);
    }
}
