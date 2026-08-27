package com.store.service;

import com.store.dto.request.discount.CreateDiscountRequest;
import com.store.dto.response.discount.DiscountValidationResult;
import com.store.entity.cart.CartItem;
import com.store.entity.category.Category;
import com.store.entity.discount.DiscountCode;
import com.store.entity.discount.DiscountStatus;
import com.store.entity.discount.DiscountType;
import com.store.entity.product.Product;
import com.store.entity.product.ProductVariant;
import com.store.exception.DuplicateResourceException;
import com.store.exception.InvalidDiscountException;
import com.store.repository.CartItemRepository;
import com.store.repository.CategoryRepository;
import com.store.repository.DiscountCodeRepository;
import com.store.repository.DiscountUsageRepository;
import com.store.service.impl.DiscountServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiscountServiceTest {

    @Mock
    private DiscountCodeRepository discountCodeRepository;

    @Mock
    private DiscountUsageRepository discountUsageRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryService categoryService;

    @Mock
    private CartItemRepository cartItemRepository;

    @InjectMocks
    private DiscountServiceImpl discountService;

    private DiscountCode percentDiscount;
    private DiscountCode fixedDiscount;
    private Category laptopCategory;
    private CartItem cartItem;

    @BeforeEach
    void setUp() {
        laptopCategory = Category.builder()
                .categoryId(1)
                .name("Laptop")
                .slug("laptop")
                .build();

        percentDiscount = DiscountCode.builder()
                .discountId(100L)
                .code("SALE10")
                .description("Giảm 10% tối đa 500k")
                .discountType(DiscountType.PERCENT)
                .discountValue(new BigDecimal("10.00"))
                .maxDiscountAmount(new BigDecimal("500000.00"))
                .minOrderValue(new BigDecimal("1000000.00"))
                .usageLimit(100)
                .usageLimitPerUser(1)
                .usedCount(0)
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(10))
                .status(DiscountStatus.ACTIVE)
                .build();

        fixedDiscount = DiscountCode.builder()
                .discountId(200L)
                .code("GIAM200K")
                .description("Giảm 200.000đ")
                .discountType(DiscountType.FIXED)
                .discountValue(new BigDecimal("200000.00"))
                .minOrderValue(new BigDecimal("500000.00"))
                .usageLimit(50)
                .usageLimitPerUser(2)
                .usedCount(5)
                .startDate(LocalDateTime.now().minusDays(2))
                .endDate(LocalDateTime.now().plusDays(5))
                .status(DiscountStatus.ACTIVE)
                .build();

        Product product = Product.builder()
                .productId(1L)
                .name("MacBook Air M3")
                .category(laptopCategory)
                .build();

        ProductVariant variant = ProductVariant.builder()
                .variantId(1L)
                .product(product)
                .variantName("16GB / 256GB")
                .price(new BigDecimal("20000000.00"))
                .build();

        cartItem = CartItem.builder()
                .cartId(1L)
                .variant(variant)
                .quantity(1)
                .build();
    }

    @Test
    @DisplayName("Validate percentage discount without hitting max cap")
    void testValidatePercent_UnderCap_Success() {
        percentDiscount.setMaxDiscountAmount(new BigDecimal("1000000.00"));
        when(discountCodeRepository.findByCodeIgnoreCase("SALE10")).thenReturn(Optional.of(percentDiscount));
        when(discountUsageRepository.countByDiscountDiscountIdAndUserUserId(100L, 1L)).thenReturn(0L);

        // Subtotal = 5,000,000đ -> 10% = 500,000đ (under cap 1,000,000đ)
        BigDecimal subtotal = new BigDecimal("5000000.00");
        DiscountValidationResult result = discountService.validateAndCalculate("SALE10", 1L, subtotal, List.of(cartItem));

        assertThat(result.isValid()).isTrue();
        assertThat(result.getDiscountAmount()).isEqualByComparingTo("500000.00");
        assertThat(result.getFinalTotal()).isEqualByComparingTo("4500000.00");
    }

    @Test
    @DisplayName("Validate percentage discount hitting max cap -> capped to maxDiscountAmount")
    void testValidatePercent_HitsMaxCap_AppliesCap() {
        percentDiscount.setMaxDiscountAmount(new BigDecimal("500000.00"));
        when(discountCodeRepository.findByCodeIgnoreCase("SALE10")).thenReturn(Optional.of(percentDiscount));
        when(discountUsageRepository.countByDiscountDiscountIdAndUserUserId(100L, 1L)).thenReturn(0L);

        // Subtotal = 20,000,000đ -> 10% = 2,000,000đ -> capped at 500,000đ
        BigDecimal subtotal = new BigDecimal("20000000.00");
        DiscountValidationResult result = discountService.validateAndCalculate("SALE10", 1L, subtotal, List.of(cartItem));

        assertThat(result.isValid()).isTrue();
        assertThat(result.getDiscountAmount()).isEqualByComparingTo("500000.00");
        assertThat(result.getFinalTotal()).isEqualByComparingTo("19500000.00");
    }

    @Test
    @DisplayName("Validate fixed amount discount -> successfully deducts fixed amount")
    void testValidateFixed_Success() {
        when(discountCodeRepository.findByCodeIgnoreCase("GIAM200K")).thenReturn(Optional.of(fixedDiscount));
        when(discountUsageRepository.countByDiscountDiscountIdAndUserUserId(200L, 1L)).thenReturn(0L);

        BigDecimal subtotal = new BigDecimal("1000000.00");
        DiscountValidationResult result = discountService.validateAndCalculate("GIAM200K", 1L, subtotal, List.of(cartItem));

        assertThat(result.isValid()).isTrue();
        assertThat(result.getDiscountAmount()).isEqualByComparingTo("200000.00");
        assertThat(result.getFinalTotal()).isEqualByComparingTo("800000.00");
    }

    @Test
    @DisplayName("Validate fixed amount discount exceeding subtotal -> caps discount to subtotal so finalTotal=0")
    void testValidateFixed_ExceedsSubtotal_CapsToSubtotal() {
        fixedDiscount.setMinOrderValue(BigDecimal.ZERO);
        when(discountCodeRepository.findByCodeIgnoreCase("GIAM200K")).thenReturn(Optional.of(fixedDiscount));
        when(discountUsageRepository.countByDiscountDiscountIdAndUserUserId(200L, 1L)).thenReturn(0L);

        BigDecimal subtotal = new BigDecimal("150000.00");
        DiscountValidationResult result = discountService.validateAndCalculate("GIAM200K", 1L, subtotal, List.of(cartItem));

        assertThat(result.isValid()).isTrue();
        assertThat(result.getDiscountAmount()).isEqualByComparingTo("150000.00");
        assertThat(result.getFinalTotal()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("Validate expired discount code -> throws InvalidDiscountException")
    void testValidate_Expired_ThrowsException() {
        percentDiscount.setEndDate(LocalDateTime.now().minusDays(1));
        when(discountCodeRepository.findByCodeIgnoreCase("SALE10")).thenReturn(Optional.of(percentDiscount));

        assertThatThrownBy(() -> discountService.validateAndCalculate("SALE10", 1L, new BigDecimal("2000000.00"), List.of(cartItem)))
                .isInstanceOf(InvalidDiscountException.class)
                .hasMessageContaining("đã hết hạn sử dụng");
    }

    @Test
    @DisplayName("Validate not yet active discount code -> throws InvalidDiscountException")
    void testValidate_NotStarted_ThrowsException() {
        percentDiscount.setStartDate(LocalDateTime.now().plusDays(2));
        when(discountCodeRepository.findByCodeIgnoreCase("SALE10")).thenReturn(Optional.of(percentDiscount));

        assertThatThrownBy(() -> discountService.validateAndCalculate("SALE10", 1L, new BigDecimal("2000000.00"), List.of(cartItem)))
                .isInstanceOf(InvalidDiscountException.class)
                .hasMessageContaining("chưa đến thời gian áp dụng");
    }

    @Test
    @DisplayName("Validate inactive discount status -> throws InvalidDiscountException")
    void testValidate_InactiveStatus_ThrowsException() {
        percentDiscount.setStatus(DiscountStatus.INACTIVE);
        when(discountCodeRepository.findByCodeIgnoreCase("SALE10")).thenReturn(Optional.of(percentDiscount));

        assertThatThrownBy(() -> discountService.validateAndCalculate("SALE10", 1L, new BigDecimal("2000000.00"), List.of(cartItem)))
                .isInstanceOf(InvalidDiscountException.class)
                .hasMessageContaining("không hoạt động");
    }

    @Test
    @DisplayName("Validate discount exceeding system usage_limit -> throws InvalidDiscountException")
    void testValidate_SystemUsageLimitExceeded_ThrowsException() {
        percentDiscount.setUsageLimit(50);
        percentDiscount.setUsedCount(50);
        when(discountCodeRepository.findByCodeIgnoreCase("SALE10")).thenReturn(Optional.of(percentDiscount));

        assertThatThrownBy(() -> discountService.validateAndCalculate("SALE10", 1L, new BigDecimal("2000000.00"), List.of(cartItem)))
                .isInstanceOf(InvalidDiscountException.class)
                .hasMessageContaining("đã hết lượt sử dụng trên hệ thống");
    }

    @Test
    @DisplayName("Validate discount exceeding per-user usage limit -> throws InvalidDiscountException")
    void testValidate_UserUsageLimitExceeded_ThrowsException() {
        percentDiscount.setUsageLimitPerUser(1);
        when(discountCodeRepository.findByCodeIgnoreCase("SALE10")).thenReturn(Optional.of(percentDiscount));
        when(discountUsageRepository.countByDiscountDiscountIdAndUserUserId(100L, 1L)).thenReturn(1L);

        assertThatThrownBy(() -> discountService.validateAndCalculate("SALE10", 1L, new BigDecimal("2000000.00"), List.of(cartItem)))
                .isInstanceOf(InvalidDiscountException.class)
                .hasMessageContaining("Bạn đã dùng hết số lượt cho phép");
    }

    @Test
    @DisplayName("Validate order subtotal less than min_order_value -> throws InvalidDiscountException")
    void testValidate_MinOrderValueNotMet_ThrowsException() {
        percentDiscount.setMinOrderValue(new BigDecimal("5000000.00"));
        when(discountCodeRepository.findByCodeIgnoreCase("SALE10")).thenReturn(Optional.of(percentDiscount));
        when(discountUsageRepository.countByDiscountDiscountIdAndUserUserId(100L, 1L)).thenReturn(0L);

        BigDecimal smallSubtotal = new BigDecimal("2000000.00");
        assertThatThrownBy(() -> discountService.validateAndCalculate("SALE10", 1L, smallSubtotal, List.of(cartItem)))
                .isInstanceOf(InvalidDiscountException.class)
                .hasMessageContaining("chưa đạt giá trị tối thiểu");
    }

    @Test
    @DisplayName("Validate category restriction with qualifying product -> calculates discount on qualifying items only")
    void testValidate_CategoryRestriction_Success() {
        percentDiscount.setApplicableCategory(laptopCategory);
        when(discountCodeRepository.findByCodeIgnoreCase("SALE10")).thenReturn(Optional.of(percentDiscount));
        when(discountUsageRepository.countByDiscountDiscountIdAndUserUserId(100L, 1L)).thenReturn(0L);
        when(categoryService.getCategoryAndDescendantIds(1)).thenReturn(Set.of(1, 6, 7));

        // Cart item belongs to category 1 (Laptop)
        BigDecimal subtotal = new BigDecimal("20000000.00");
        DiscountValidationResult result = discountService.validateAndCalculate("SALE10", 1L, subtotal, List.of(cartItem));

        assertThat(result.isValid()).isTrue();
        assertThat(result.getDiscountAmount()).isEqualByComparingTo("500000.00"); // capped at 500k
    }

    @Test
    @DisplayName("Validate category restriction with no qualifying product in cart -> throws InvalidDiscountException")
    void testValidate_CategoryRestriction_NoMatchingItem_ThrowsException() {
        Category accessoryCat = Category.builder().categoryId(5).name("Phụ kiện").build();
        percentDiscount.setApplicableCategory(accessoryCat);
        when(discountCodeRepository.findByCodeIgnoreCase("SALE10")).thenReturn(Optional.of(percentDiscount));
        when(discountUsageRepository.countByDiscountDiscountIdAndUserUserId(100L, 1L)).thenReturn(0L);
        when(categoryService.getCategoryAndDescendantIds(5)).thenReturn(Set.of(5, 27, 28));

        // Cart item belongs to category 1 (Laptop), not category 5
        BigDecimal subtotal = new BigDecimal("20000000.00");
        assertThatThrownBy(() -> discountService.validateAndCalculate("SALE10", 1L, subtotal, List.of(cartItem)))
                .isInstanceOf(InvalidDiscountException.class)
                .hasMessageContaining("chỉ áp dụng cho nhóm ngành hàng 'Phụ kiện'");
    }

    @Test
    @DisplayName("Validate discount should reject when userId is null (guest user)")
    void testValidateAndCalculate_NullUserId_ThrowsInvalidDiscountException() {
        BigDecimal subtotal = new BigDecimal("20000000.00");
        assertThatThrownBy(() -> discountService.validateAndCalculate("SALE10", null, subtotal, List.of(cartItem)))
                .isInstanceOf(InvalidDiscountException.class)
                .hasMessageContaining("Mã giảm giá chỉ dành riêng cho thành viên đã đăng nhập tài khoản");
    }

    @Test
    @DisplayName("Create discount with duplicate code -> throws DuplicateResourceException")
    void testCreateDiscount_DuplicateCode_ThrowsException() {
        CreateDiscountRequest req = CreateDiscountRequest.builder()
                .code("SALE10")
                .discountType(DiscountType.PERCENT)
                .discountValue(new BigDecimal("10.00"))
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(5))
                .build();

        when(discountCodeRepository.existsByCodeIgnoreCase("SALE10")).thenReturn(true);

        assertThatThrownBy(() -> discountService.createDiscount(req))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("đã tồn tại");

        verify(discountCodeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Create discount with endDate before startDate -> throws IllegalArgumentException")
    void testCreateDiscount_EndDateBeforeStartDate_ThrowsException() {
        CreateDiscountRequest req = CreateDiscountRequest.builder()
                .code("SALE99")
                .discountType(DiscountType.FIXED)
                .discountValue(new BigDecimal("100000.00"))
                .startDate(LocalDateTime.now().plusDays(5))
                .endDate(LocalDateTime.now().plusDays(2))
                .build();

        when(discountCodeRepository.existsByCodeIgnoreCase("SALE99")).thenReturn(false);

        assertThatThrownBy(() -> discountService.createDiscount(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ngày kết thúc hiệu lực phải sau ngày bắt đầu");
    }

    @Test
    @DisplayName("Admin getDiscounts with pagination and specification -> returns PageResponse of DiscountResponse")
    void testGetAdminDiscounts_ReturnsPagedResult() {
        com.store.dto.request.discount.DiscountFilterRequest filter = com.store.dto.request.discount.DiscountFilterRequest.builder()
                .page(0)
                .size(10)
                .keyword("SALE")
                .status(DiscountStatus.ACTIVE)
                .discountType(DiscountType.PERCENT)
                .build();

        org.springframework.data.domain.Page<DiscountCode> mockPage = new org.springframework.data.domain.PageImpl<>(
                List.of(percentDiscount),
                org.springframework.data.domain.PageRequest.of(0, 10),
                1
        );

        when(discountCodeRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(mockPage);

        com.store.dto.response.PageResponse<com.store.dto.response.discount.DiscountResponse> result = discountService.getAdminDiscounts(filter);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCode()).isEqualTo("SALE10");
        assertThat(result.getTotalElements()).isEqualTo(1);
    }
}
