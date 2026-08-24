package com.store.repository;

import com.store.entity.cart.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    @Query("SELECT c FROM CartItem c JOIN FETCH c.variant v JOIN FETCH v.product p " +
           "WHERE c.user.userId = :userId ORDER BY c.createdAt DESC")
    List<CartItem> findByUserIdWithDetails(@Param("userId") Long userId);

    Optional<CartItem> findByUserUserIdAndVariantVariantId(Long userId, Long variantId);

    Optional<CartItem> findByCartIdAndUserUserId(Long cartId, Long userId);

    @Modifying
    @Query(value = "INSERT INTO cart_items (user_id, variant_id, quantity, created_at) " +
                   "VALUES (:userId, :variantId, :qty, NOW()) " +
                   "ON DUPLICATE KEY UPDATE quantity = quantity + :qty", nativeQuery = true)
    int upsertCartItemAtomic(@Param("userId") Long userId, @Param("variantId") Long variantId, @Param("qty") int qty);

    @Modifying
    @Query("DELETE FROM CartItem c WHERE c.user.userId = :userId AND c.cartId = :cartId")
    void deleteByUserUserIdAndCartId(@Param("userId") Long userId, @Param("cartId") Long cartId);

    @Modifying
    @Query("DELETE FROM CartItem c WHERE c.user.userId = :userId")
    void deleteByUserUserId(@Param("userId") Long userId);

    long countByUserUserId(Long userId);
}
