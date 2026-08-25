package com.store.service;

import com.store.dto.request.discount.CreateDiscountRequest;
import com.store.dto.request.discount.DiscountFilterRequest;
import com.store.dto.request.discount.UpdateDiscountRequest;
import com.store.dto.response.PageResponse;
import com.store.dto.response.discount.DiscountMetricsResponse;
import com.store.dto.response.discount.DiscountResponse;
import com.store.dto.response.discount.DiscountUsageResponse;
import com.store.dto.response.discount.DiscountValidationResult;
import com.store.entity.cart.CartItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface DiscountService {

    /** Validate discount code for currently logged in customer using their actual cart */
    DiscountValidationResult validateDiscountForCustomer(Long userId, String code);

    /** Core validation and calculation engine */
    DiscountValidationResult validateAndCalculate(String code, Long userId, BigDecimal subtotal, List<CartItem> cartItems);

    /** Get active discounts visible to storefront customers */
    List<DiscountResponse> getPublicActiveDiscounts();

    /** Admin: Paginated and filtered discount list */
    PageResponse<DiscountResponse> getAdminDiscounts(DiscountFilterRequest filter);

    /** Admin: Get discount by ID */
    DiscountResponse getDiscountById(Long id);

    /** Admin: Create discount code */
    DiscountResponse createDiscount(CreateDiscountRequest request);

    /** Admin: Update discount code */
    DiscountResponse updateDiscount(Long id, UpdateDiscountRequest request);

    /** Admin: Soft-delete / deactivate discount code */
    void deleteDiscount(Long id);

    /** Admin: Get discount metrics */
    DiscountMetricsResponse getMetrics();

    /** Admin: Get usage history for a discount code */
    List<DiscountUsageResponse> getDiscountUsages(Long discountId);

    /** Atomic increment of used_count during checkout */
    int incrementUsedCountAtomic(Long discountId, LocalDateTime now);

    /** Rollback used_count and remove usage record when order is cancelled */
    void rollbackDiscountUsage(Long discountId, Long orderId);
}
