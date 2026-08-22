package com.store.service;

import com.store.dto.request.ProductVariantRequest;
import com.store.dto.response.ProductVariantResponse;
import com.store.entity.product.Product;
import com.store.entity.product.ProductVariant;
import com.store.entity.product.ProductVariantStatus;
import com.store.exception.DuplicateResourceException;
import com.store.exception.ResourceNotFoundException;
import com.store.repository.ProductRepository;
import com.store.repository.ProductVariantRepository;
import com.store.service.impl.ProductVariantServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductVariantServiceTest {

    @Mock
    private ProductVariantRepository productVariantRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductVariantServiceImpl productVariantService;

    private Product testProduct;
    private ProductVariant testVariant;

    @BeforeEach
    void setUp() {
        testProduct = Product.builder()
                .productId(1L)
                .name("RAM Corsair Vengeance RGB DDR5")
                .sku("RAM-CORSAIR-DDR5")
                .build();

        testVariant = ProductVariant.builder()
                .variantId(10L)
                .product(testProduct)
                .variantName("32GB (2x16GB) 6000MHz Đen")
                .skuVariant("RAM-CORSAIR-32GB-BLK")
                .price(new BigDecimal("3200000.00"))
                .salePrice(new BigDecimal("2990000.00"))
                .costPrice(new BigDecimal("2500000.00"))
                .status(ProductVariantStatus.ACTIVE)
                .build();
    }

    @Nested
    @DisplayName("Read Operations")
    class ReadTests {

        @Test
        @DisplayName("getVariantsByProductId should return list when product exists")
        void getVariantsByProductId_success() {
            when(productRepository.existsById(1L)).thenReturn(true);
            when(productVariantRepository.findByProduct_ProductIdOrderByPriceAsc(1L)).thenReturn(List.of(testVariant));

            List<ProductVariantResponse> result = productVariantService.getVariantsByProductId(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getVariantName()).isEqualTo("32GB (2x16GB) 6000MHz Đen");
            assertThat(result.get(0).getProductId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("getVariantsByProductId should throw ResourceNotFoundException when product does not exist")
        void getVariantsByProductId_notFound() {
            when(productRepository.existsById(99L)).thenReturn(false);

            assertThatThrownBy(() -> productVariantService.getVariantsByProductId(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Product not found with id: 99");
        }

        @Test
        @DisplayName("getVariantById should return variant when found")
        void getVariantById_found() {
            when(productVariantRepository.findById(10L)).thenReturn(Optional.of(testVariant));

            ProductVariantResponse response = productVariantService.getVariantById(10L);

            assertThat(response).isNotNull();
            assertThat(response.getVariantId()).isEqualTo(10L);
            assertThat(response.getPrice()).isEqualTo(new BigDecimal("3200000.00"));
        }
    }

    @Nested
    @DisplayName("Create Operations")
    class CreateTests {

        @Test
        @DisplayName("createVariant should succeed when valid")
        void createVariant_success() {
            ProductVariantRequest request = ProductVariantRequest.builder()
                    .variantName("64GB (2x32GB) 6000MHz Trắng")
                    .skuVariant("RAM-CORSAIR-64GB-WHT")
                    .price(new BigDecimal("6000000.00"))
                    .salePrice(new BigDecimal("5700000.00"))
                    .status("active")
                    .build();

            when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
            when(productVariantRepository.existsBySkuVariant("RAM-CORSAIR-64GB-WHT")).thenReturn(false);
            when(productVariantRepository.save(any(ProductVariant.class))).thenAnswer(inv -> {
                ProductVariant v = inv.getArgument(0);
                v.setVariantId(11L);
                return v;
            });

            ProductVariantResponse created = productVariantService.createVariant(1L, request);

            assertThat(created).isNotNull();
            assertThat(created.getVariantId()).isEqualTo(11L);
            assertThat(created.getVariantName()).isEqualTo("64GB (2x32GB) 6000MHz Trắng");
            assertThat(created.getStatus()).isEqualTo("active");
        }

        @Test
        @DisplayName("createVariant when salePrice > price should throw IllegalArgumentException")
        void createVariant_salePriceGreaterThanPrice_shouldThrow() {
            ProductVariantRequest request = ProductVariantRequest.builder()
                    .variantName("32GB")
                    .price(new BigDecimal("1000000.00"))
                    .salePrice(new BigDecimal("1500000.00"))
                    .build();

            when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

            assertThatThrownBy(() -> productVariantService.createVariant(1L, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Sale price cannot be greater than regular price.");
        }

        @Test
        @DisplayName("createVariant when duplicate SKU should throw DuplicateResourceException")
        void createVariant_duplicateSku_shouldThrow() {
            ProductVariantRequest request = ProductVariantRequest.builder()
                    .variantName("32GB Đen")
                    .skuVariant("RAM-CORSAIR-32GB-BLK")
                    .price(new BigDecimal("3200000.00"))
                    .build();

            when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
            when(productVariantRepository.existsBySkuVariant("RAM-CORSAIR-32GB-BLK")).thenReturn(true);

            assertThatThrownBy(() -> productVariantService.createVariant(1L, request))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("Product variant already exists with SKU");
        }
    }

    @Nested
    @DisplayName("Update and Delete Operations")
    class UpdateDeleteTests {

        @Test
        @DisplayName("updateVariant should succeed when valid")
        void updateVariant_success() {
            ProductVariantRequest request = ProductVariantRequest.builder()
                    .variantName("32GB (2x16GB) 6000MHz Đen RGB")
                    .skuVariant("RAM-CORSAIR-32GB-BLK")
                    .price(new BigDecimal("3100000.00"))
                    .status("active")
                    .build();

            when(productVariantRepository.findById(10L)).thenReturn(Optional.of(testVariant));
            when(productVariantRepository.existsBySkuVariantAndVariantIdNot("RAM-CORSAIR-32GB-BLK", 10L)).thenReturn(false);
            when(productVariantRepository.save(any(ProductVariant.class))).thenReturn(testVariant);

            ProductVariantResponse updated = productVariantService.updateVariant(10L, request);

            assertThat(updated).isNotNull();
            assertThat(testVariant.getVariantName()).isEqualTo("32GB (2x16GB) 6000MHz Đen RGB");
        }

        @Test
        @DisplayName("deleteVariant should delete when found")
        void deleteVariant_success() {
            when(productVariantRepository.findById(10L)).thenReturn(Optional.of(testVariant));

            productVariantService.deleteVariant(10L);

            verify(productVariantRepository).delete(testVariant);
        }

        @Test
        @DisplayName("deleteVariant should throw ResourceNotFoundException when not found")
        void deleteVariant_notFound() {
            when(productVariantRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productVariantService.deleteVariant(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Product variant not found with id: 99");

            verify(productVariantRepository, never()).delete(any(ProductVariant.class));
        }
    }
}
