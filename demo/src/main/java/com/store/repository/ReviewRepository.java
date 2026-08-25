package com.store.repository;

import com.store.entity.review.Review;
import com.store.entity.review.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long>, JpaSpecificationExecutor<Review> {

    @Query("SELECT r.rating, COUNT(r) FROM Review r " +
           "WHERE r.product.productId = :productId AND r.status = com.store.entity.review.ReviewStatus.APPROVED " +
           "GROUP BY r.rating")
    List<Object[]> countReviewsGroupByRating(@Param("productId") Long productId);

    @Query("SELECT COUNT(oi) > 0 FROM OrderItem oi " +
           "JOIN oi.order o JOIN oi.variant pv " +
           "WHERE o.user.userId = :userId " +
           "AND pv.product.productId = :productId " +
           "AND o.orderStatus = com.store.entity.order.OrderStatus.COMPLETED")
    boolean hasUserPurchasedProduct(@Param("userId") Long userId, @Param("productId") Long productId);

    Page<Review> findByProduct_ProductIdAndStatusOrderByCreatedAtDesc(Long productId, ReviewStatus status, Pageable pageable);

    Page<Review> findByProduct_ProductIdAndRatingAndStatusOrderByCreatedAtDesc(Long productId, Integer rating, ReviewStatus status, Pageable pageable);

    Optional<Review> findByUser_UserIdAndProduct_ProductId(Long userId, Long productId);

    long countByStatus(ReviewStatus status);
}
