package com.store.service.impl;

import com.store.dto.request.review.CreateReviewRequest;
import com.store.dto.request.review.ReviewFilterRequest;
import com.store.dto.response.PageResponse;
import com.store.dto.response.review.RatingSummaryResponse;
import com.store.dto.response.review.ReviewResponse;
import com.store.entity.product.Product;
import com.store.entity.review.Review;
import com.store.entity.review.ReviewStatus;
import com.store.entity.user.User;
import com.store.exception.ResourceNotFoundException;
import com.store.repository.ProductRepository;
import com.store.repository.ReviewRepository;
import com.store.repository.UserRepository;
import com.store.service.ReviewService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(cacheNames = "productRatingSummary", key = "#productId"),
        @CacheEvict(cacheNames = "productReviews", allEntries = true)
    })
    public ReviewResponse submitReview(Long userId, Long productId, CreateReviewRequest request) {
        log.info("User {} submitting review for product {}", userId, productId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với id: " + productId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với id: " + userId));

        // Logic check verified purchase across all variants belonging to this product
        boolean isVerifiedPurchase = reviewRepository.hasUserPurchasedProduct(userId, productId);

        // Upsert review (Update if already reviewed by this user, else create new)
        Review review = reviewRepository.findByUser_UserIdAndProduct_ProductId(userId, productId)
                .map(existing -> {
                    log.info("Updating existing review id {} for user {} on product {}", existing.getReviewId(), userId, productId);
                    existing.setRating(request.getRating());
                    existing.setTitle(request.getTitle());
                    existing.setComment(request.getComment());
                    existing.setIsVerifiedPurchase(isVerifiedPurchase);
                    existing.setStatus(ReviewStatus.APPROVED);
                    return existing;
                })
                .orElseGet(() -> {
                    log.info("Creating new review for user {} on product {}", userId, productId);
                    return Review.builder()
                            .product(product)
                            .user(user)
                            .rating(request.getRating())
                            .title(request.getTitle())
                            .comment(request.getComment())
                            .isVerifiedPurchase(isVerifiedPurchase)
                            .status(ReviewStatus.APPROVED)
                            .build();
                });

        Review savedReview = reviewRepository.save(review);
        return ReviewResponse.fromEntity(savedReview);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "productRatingSummary", key = "#productId")
    public RatingSummaryResponse getProductRatingSummary(Long productId) {
        log.info("Computing product rating summary for product id: {}", productId);

        // 1 single aggregate query with GROUP BY rating
        List<Object[]> rows = reviewRepository.countReviewsGroupByRating(productId);

        Map<Integer, Long> countsMap = new HashMap<>();
        for (int star = 1; star <= 5; star++) {
            countsMap.put(star, 0L);
        }

        long totalReviews = 0L;
        long weightedSum = 0L;

        for (Object[] row : rows) {
            Integer rating = ((Number) row[0]).intValue();
            Long count = ((Number) row[1]).longValue();
            countsMap.put(rating, count);
            totalReviews += count;
            weightedSum += (rating * count);
        }

        double averageRating = 0.0;
        Map<Integer, Double> starPercentages = new HashMap<>();

        if (totalReviews > 0) {
            averageRating = BigDecimal.valueOf((double) weightedSum / totalReviews)
                    .setScale(1, RoundingMode.HALF_UP)
                    .doubleValue();

            for (int star = 1; star <= 5; star++) {
                double pct = ((double) countsMap.get(star) / totalReviews) * 100.0;
                starPercentages.put(star, BigDecimal.valueOf(pct).setScale(1, RoundingMode.HALF_UP).doubleValue());
            }
        } else {
            for (int star = 1; star <= 5; star++) {
                starPercentages.put(star, 0.0);
            }
        }

        return RatingSummaryResponse.builder()
                .productId(productId)
                .averageRating(averageRating)
                .totalReviews(totalReviews)
                .ratingCounts(countsMap)
                .starPercentages(starPercentages)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getProductReviews(Long productId, Integer ratingFilter, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Review> reviewPage;

        if (ratingFilter != null && ratingFilter >= 1 && ratingFilter <= 5) {
            reviewPage = reviewRepository.findByProduct_ProductIdAndRatingAndStatusOrderByCreatedAtDesc(
                    productId, ratingFilter, ReviewStatus.APPROVED, pageable);
        } else {
            reviewPage = reviewRepository.findByProduct_ProductIdAndStatusOrderByCreatedAtDesc(
                    productId, ReviewStatus.APPROVED, pageable);
        }

        return PageResponse.of(reviewPage.map(ReviewResponse::fromEntity));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getAdminReviews(ReviewFilterRequest filter) {
        Sort sort = Sort.by(
                "asc".equalsIgnoreCase(filter.getSortDir()) ? Sort.Direction.ASC : Sort.Direction.DESC,
                filter.getSortBy() != null ? filter.getSortBy() : "createdAt"
        );
        Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize(), sort);

        Specification<Review> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.getProductId() != null) {
                predicates.add(cb.equal(root.get("product").get("productId"), filter.getProductId()));
            }
            if (filter.getRating() != null) {
                predicates.add(cb.equal(root.get("rating"), filter.getRating()));
            }
            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }
            if (filter.getKeyword() != null && !filter.getKeyword().isBlank()) {
                String kw = "%" + filter.getKeyword().trim().toLowerCase() + "%";
                Predicate titlePred = cb.like(cb.lower(root.get("title")), kw);
                Predicate commentPred = cb.like(cb.lower(root.get("comment")), kw);
                Predicate userPred = cb.like(cb.lower(root.get("user").get("fullName")), kw);
                Predicate prodPred = cb.like(cb.lower(root.get("product").get("name")), kw);
                predicates.add(cb.or(titlePred, commentPred, userPred, prodPred));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Review> page = reviewRepository.findAll(spec, pageable);
        return PageResponse.of(page.map(ReviewResponse::fromEntity));
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(cacheNames = "productRatingSummary", allEntries = true),
        @CacheEvict(cacheNames = "productReviews", allEntries = true)
    })
    public ReviewResponse updateReviewStatus(Long reviewId, ReviewStatus status) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đánh giá với id: " + reviewId));
        review.setStatus(status);
        Review saved = reviewRepository.save(review);
        return ReviewResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(cacheNames = "productRatingSummary", allEntries = true),
        @CacheEvict(cacheNames = "productReviews", allEntries = true)
    })
    public void deleteReview(Long reviewId) {
        if (!reviewRepository.existsById(reviewId)) {
            throw new ResourceNotFoundException("Không tìm thấy đánh giá với id: " + reviewId);
        }
        reviewRepository.deleteById(reviewId);
    }
}
