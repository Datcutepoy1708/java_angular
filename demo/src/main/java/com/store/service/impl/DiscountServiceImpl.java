package com.store.service.impl;

import com.store.dto.request.discount.CreateDiscountRequest;
import com.store.dto.request.discount.DiscountFilterRequest;
import com.store.dto.request.discount.UpdateDiscountRequest;
import com.store.dto.response.PageResponse;
import com.store.dto.response.discount.DiscountMetricsResponse;
import com.store.dto.response.discount.DiscountResponse;
import com.store.dto.response.discount.DiscountUsageResponse;
import com.store.dto.response.discount.DiscountValidationResult;
import com.store.entity.cart.CartItem;
import com.store.entity.category.Category;
import com.store.entity.discount.DiscountCode;
import com.store.entity.discount.DiscountStatus;
import com.store.entity.discount.DiscountType;
import com.store.entity.discount.DiscountUsage;
import com.store.exception.DuplicateResourceException;
import com.store.exception.InvalidDiscountException;
import com.store.exception.ResourceNotFoundException;
import com.store.repository.CartItemRepository;
import com.store.repository.CategoryRepository;
import com.store.repository.DiscountCodeRepository;
import com.store.repository.DiscountUsageRepository;
import com.store.service.CategoryService;
import com.store.service.DiscountService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiscountServiceImpl implements DiscountService {

    private final DiscountCodeRepository discountCodeRepository;
    private final DiscountUsageRepository discountUsageRepository;
    private final CategoryRepository categoryRepository;
    private final CategoryService categoryService;
    private final CartItemRepository cartItemRepository;

    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,### đ");

    @Override
    @Transactional(readOnly = true)
    public DiscountValidationResult validateDiscountForCustomer(Long userId, String code) {
        List<CartItem> cartItems = cartItemRepository.findByUserIdWithDetails(userId);
        if (cartItems.isEmpty()) {
            throw new InvalidDiscountException("Giỏ hàng của bạn đang trống, không thể áp dụng mã giảm giá.");
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem item : cartItems) {
            BigDecimal price = item.getVariant().getPrice();
            subtotal = subtotal.add(price.multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        return validateAndCalculate(code, userId, subtotal, cartItems);
    }

    @Override
    @Transactional(readOnly = true)
    public DiscountValidationResult validateAndCalculate(String code, Long userId, BigDecimal subtotal, List<CartItem> cartItems) {
        if (code == null || code.isBlank()) {
            throw new InvalidDiscountException("Mã giảm giá không được để trống.");
        }

        String cleanCode = code.trim().toUpperCase();
        DiscountCode discount = discountCodeRepository.findByCodeIgnoreCase(cleanCode)
                .orElseThrow(() -> new InvalidDiscountException("Mã giảm giá '" + cleanCode + "' không tồn tại."));

        if (discount.getStatus() != DiscountStatus.ACTIVE) {
            throw new InvalidDiscountException("Mã giảm giá '" + cleanCode + "' hiện không hoạt động hoặc đã bị vô hiệu hóa.");
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(discount.getStartDate())) {
            throw new InvalidDiscountException("Mã giảm giá '" + cleanCode + "' chưa đến thời gian áp dụng.");
        }
        if (now.isAfter(discount.getEndDate())) {
            throw new InvalidDiscountException("Mã giảm giá '" + cleanCode + "' đã hết hạn sử dụng.");
        }

        // Usage limit across system
        if (discount.getUsageLimit() != null && discount.getUsedCount() >= discount.getUsageLimit()) {
            throw new InvalidDiscountException("Mã giảm giá '" + cleanCode + "' đã hết lượt sử dụng trên hệ thống.");
        }

        // Usage limit per user
        if (userId != null && discount.getUsageLimitPerUser() != null) {
            long userUsed = discountUsageRepository.countByDiscountDiscountIdAndUserUserId(discount.getDiscountId(), userId);
            if (userUsed >= discount.getUsageLimitPerUser()) {
                throw new InvalidDiscountException("Bạn đã dùng hết số lượt cho phép (" + discount.getUsageLimitPerUser() + " lượt) của mã giảm giá này.");
            }
        }

        // Minimum order value
        if (discount.getMinOrderValue() != null && subtotal.compareTo(discount.getMinOrderValue()) < 0) {
            throw new InvalidDiscountException("Đơn hàng chưa đạt giá trị tối thiểu " +
                    MONEY_FORMAT.format(discount.getMinOrderValue()) + " để áp dụng mã giảm giá này.");
        }

        // Category restriction check
        BigDecimal baseAmount = subtotal;
        if (discount.getApplicableCategory() != null) {
            Integer catId = discount.getApplicableCategory().getCategoryId();
            Set<Integer> validCategoryIds = categoryService.getCategoryAndDescendantIds(catId);
            BigDecimal qualifyingSubtotal = BigDecimal.ZERO;
            boolean hasQualifyingItem = false;

            if (cartItems != null && !cartItems.isEmpty()) {
                for (CartItem item : cartItems) {
                    if (item.getVariant() != null && item.getVariant().getProduct() != null && item.getVariant().getProduct().getCategory() != null) {
                        Integer itemCatId = item.getVariant().getProduct().getCategory().getCategoryId();
                        if (validCategoryIds.contains(itemCatId)) {
                            hasQualifyingItem = true;
                            BigDecimal itemPrice = item.getVariant().getPrice();
                            BigDecimal itemTotal = itemPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
                            qualifyingSubtotal = qualifyingSubtotal.add(itemTotal);
                        }
                    }
                }
            }

            if (!hasQualifyingItem) {
                throw new InvalidDiscountException("Mã giảm giá này chỉ áp dụng cho nhóm ngành hàng '" +
                        discount.getApplicableCategory().getName() + "' hoặc các danh mục con liên quan.");
            }
            baseAmount = qualifyingSubtotal;
        }

        // Calculation of discount amount
        BigDecimal discountAmount = BigDecimal.ZERO;
        if (discount.getDiscountType() == DiscountType.PERCENT) {
            BigDecimal percentMultiplier = discount.getDiscountValue().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            BigDecimal calculated = baseAmount.multiply(percentMultiplier).setScale(2, RoundingMode.HALF_UP);
            if (discount.getMaxDiscountAmount() != null && calculated.compareTo(discount.getMaxDiscountAmount()) > 0) {
                discountAmount = discount.getMaxDiscountAmount();
            } else {
                discountAmount = calculated;
            }
        } else if (discount.getDiscountType() == DiscountType.FIXED) {
            discountAmount = discount.getDiscountValue().min(baseAmount);
        }

        // Capped so discount never exceeds total subtotal
        discountAmount = discountAmount.min(subtotal);
        BigDecimal finalTotal = subtotal.subtract(discountAmount).max(BigDecimal.ZERO);

        return DiscountValidationResult.builder()
                .valid(true)
                .discountId(discount.getDiscountId())
                .code(discount.getCode())
                .discountType(discount.getDiscountType())
                .discountValue(discount.getDiscountValue())
                .discountAmount(discountAmount)
                .subtotal(subtotal)
                .finalTotal(finalTotal)
                .description(discount.getDescription())
                .message("Áp dụng mã giảm giá '" + discount.getCode() + "' thành công!")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DiscountResponse> getPublicActiveDiscounts() {
        LocalDateTime now = LocalDateTime.now();
        return discountCodeRepository.findActivePublicDiscounts(now).stream()
                .map(DiscountResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DiscountResponse> getAdminDiscounts(DiscountFilterRequest filter) {
        Sort sort = Sort.by(
                "desc".equalsIgnoreCase(filter.getSortDir()) ? Sort.Direction.DESC : Sort.Direction.ASC,
                filter.getSortBy() != null ? filter.getSortBy() : "createdAt"
        );
        Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize(), sort);

        Specification<DiscountCode> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.getKeyword() != null && !filter.getKeyword().isBlank()) {
                String kw = "%" + filter.getKeyword().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("code")), kw),
                        cb.like(cb.lower(root.get("description")), kw)
                ));
            }

            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }

            if (filter.getDiscountType() != null) {
                predicates.add(cb.equal(root.get("discountType"), filter.getDiscountType()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<DiscountCode> page = discountCodeRepository.findAll(spec, pageable);
        return PageResponse.of(page.map(DiscountResponse::fromEntity));
    }

    @Override
    @Transactional(readOnly = true)
    public DiscountResponse getDiscountById(Long id) {
        DiscountCode discount = discountCodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Discount not found with id: " + id));
        return DiscountResponse.fromEntity(discount);
    }

    @Override
    @Transactional
    public DiscountResponse createDiscount(CreateDiscountRequest request) {
        String code = request.getCode().trim().toUpperCase();
        if (discountCodeRepository.existsByCodeIgnoreCase(code)) {
            throw new DuplicateResourceException("Mã giảm giá '" + code + "' đã tồn tại trên hệ thống.");
        }

        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new IllegalArgumentException("Ngày kết thúc hiệu lực phải sau ngày bắt đầu.");
        }

        if (request.getDiscountType() == DiscountType.PERCENT && request.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("Giá trị giảm theo phần trăm không được vượt quá 100%.");
        }

        Category category = null;
        if (request.getApplicableCategoryId() != null) {
            category = categoryRepository.findById(request.getApplicableCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.getApplicableCategoryId()));
        }

        DiscountCode discount = DiscountCode.builder()
                .code(code)
                .description(request.getDescription())
                .discountType(request.getDiscountType())
                .discountValue(request.getDiscountValue())
                .maxDiscountAmount(request.getMaxDiscountAmount())
                .minOrderValue(request.getMinOrderValue() != null ? request.getMinOrderValue() : BigDecimal.ZERO)
                .usageLimit(request.getUsageLimit())
                .usageLimitPerUser(request.getUsageLimitPerUser() != null ? request.getUsageLimitPerUser() : 1)
                .usedCount(0)
                .applicableCategory(category)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(request.getStatus() != null ? request.getStatus() : DiscountStatus.ACTIVE)
                .build();

        return DiscountResponse.fromEntity(discountCodeRepository.save(discount));
    }

    @Override
    @Transactional
    public DiscountResponse updateDiscount(Long id, UpdateDiscountRequest request) {
        DiscountCode discount = discountCodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Discount not found with id: " + id));

        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new IllegalArgumentException("Ngày kết thúc hiệu lực phải sau ngày bắt đầu.");
        }

        if (request.getDiscountType() == DiscountType.PERCENT && request.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("Giá trị giảm theo phần trăm không được vượt quá 100%.");
        }

        Category category = null;
        if (request.getApplicableCategoryId() != null) {
            category = categoryRepository.findById(request.getApplicableCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.getApplicableCategoryId()));
        }

        discount.setDescription(request.getDescription());
        discount.setDiscountType(request.getDiscountType());
        discount.setDiscountValue(request.getDiscountValue());
        discount.setMaxDiscountAmount(request.getMaxDiscountAmount());
        discount.setMinOrderValue(request.getMinOrderValue() != null ? request.getMinOrderValue() : BigDecimal.ZERO);
        discount.setUsageLimit(request.getUsageLimit());
        discount.setUsageLimitPerUser(request.getUsageLimitPerUser() != null ? request.getUsageLimitPerUser() : 1);
        discount.setApplicableCategory(category);
        discount.setStartDate(request.getStartDate());
        discount.setEndDate(request.getEndDate());
        discount.setStatus(request.getStatus());

        return DiscountResponse.fromEntity(discountCodeRepository.save(discount));
    }

    @Override
    @Transactional
    public void deleteDiscount(Long id) {
        DiscountCode discount = discountCodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Discount not found with id: " + id));
        discount.setStatus(DiscountStatus.INACTIVE);
        discountCodeRepository.save(discount);
        log.info("Soft-deactivated discount id {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public DiscountMetricsResponse getMetrics() {
        LocalDateTime now = LocalDateTime.now();
        long total = discountCodeRepository.count();
        long active = discountCodeRepository.countByStatus(DiscountStatus.ACTIVE);
        long totalUsed = discountCodeRepository.sumAllUsedCount();
        long expired = discountCodeRepository.countExpiredDiscounts(now);

        return DiscountMetricsResponse.builder()
                .totalDiscounts(total)
                .activeDiscounts(active)
                .totalUsedCount(totalUsed)
                .expiredDiscounts(expired)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DiscountUsageResponse> getDiscountUsages(Long discountId) {
        if (!discountCodeRepository.existsById(discountId)) {
            throw new ResourceNotFoundException("Discount not found with id: " + discountId);
        }
        return discountUsageRepository.findByDiscountDiscountIdOrderByUsedAtDesc(discountId).stream()
                .map(DiscountUsageResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional
    public int incrementUsedCountAtomic(Long discountId, LocalDateTime now) {
        return discountCodeRepository.incrementUsedCountAtomic(discountId, now);
    }

    @Override
    @Transactional
    public void rollbackDiscountUsage(Long discountId, Long orderId) {
        log.info("Rolling back discount usage for discountId {} and orderId {}", discountId, orderId);
        if (discountId != null) {
            discountCodeRepository.decrementUsedCountAtomic(discountId);
        }
        if (orderId != null) {
            discountUsageRepository.deleteByOrderOrderId(orderId);
        }
    }
}
