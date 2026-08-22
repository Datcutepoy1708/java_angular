package com.store.service;

import com.store.dto.request.ProductImageRequest;
import com.store.dto.response.ProductImageResponse;
import com.store.entity.product.ImageType;
import com.store.entity.product.Product;
import com.store.entity.product.ProductImage;
import com.store.entity.product.ProductVariant;
import com.store.exception.ResourceNotFoundException;
import com.store.repository.ProductImageRepository;
import com.store.repository.ProductRepository;
import com.store.repository.ProductVariantRepository;
import com.store.service.impl.ProductImageServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductImageServiceTest {

    @Mock
    private ProductImageRepository productImageRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @InjectMocks
    private ProductImageServiceImpl productImageService;

    private Product testProduct;
    private ProductVariant testVariant;
    private ProductImage testMainImage;
    private ProductImage testSubImage;

    @BeforeEach
    void setUp() {
        testProduct = Product.builder()
                .productId(1L)
                .name("Card màn hình ASUS ROG Strix RTX 4090")
                .sku("VGA-ASUS-4090")
                .build();

        testVariant = ProductVariant.builder()
                .variantId(10L)
                .product(testProduct)
                .variantName("Bản OC 24GB")
                .build();

        testMainImage = ProductImage.builder()
                .imageId(100L)
                .product(testProduct)
                .variant(null)
                .imageUrl("https://example.com/rtx4090-main.png")
                .imageType(ImageType.MAIN)
                .sortOrder(0)
                .altText("RTX 4090 Main Image")
                .build();

        testSubImage = ProductImage.builder()
                .imageId(101L)
                .product(testProduct)
                .variant(null)
                .imageUrl("https://example.com/rtx4090-side.png")
                .imageType(ImageType.SUB)
                .sortOrder(1)
                .altText("RTX 4090 Side Image")
                .build();
    }

    @Nested
    @DisplayName("Read Operations")
    class ReadTests {

        @Test
        @DisplayName("getImagesByProductId should return list when product exists")
        void getImagesByProductId_success() {
            when(productRepository.existsById(1L)).thenReturn(true);
            when(productImageRepository.findByProduct_ProductIdOrderBySortOrderAscImageIdAsc(1L))
                    .thenReturn(List.of(testMainImage, testSubImage));

            List<ProductImageResponse> result = productImageService.getImagesByProductId(1L);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getImageType()).isEqualTo("main");
            assertThat(result.get(1).getImageType()).isEqualTo("sub");
        }

        @Test
        @DisplayName("getImagesByProductId should throw ResourceNotFoundException when product does not exist")
        void getImagesByProductId_notFound() {
            when(productRepository.existsById(99L)).thenReturn(false);

            assertThatThrownBy(() -> productImageService.getImagesByProductId(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Product not found with id: 99");
        }
    }

    @Nested
    @DisplayName("Create & Set Main Operations")
    class CreateTests {

        @Test
        @DisplayName("addImage as MAIN should demote existing main images and save new image")
        void addImage_asMain_shouldDemoteOldMain() {
            ProductImageRequest request = ProductImageRequest.builder()
                    .imageUrl("https://example.com/rtx4090-new-main.png")
                    .imageType("main")
                    .sortOrder(0)
                    .altText("New Main Cover")
                    .build();

            when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
            when(productImageRepository.findByProduct_ProductIdAndVariantIsNullAndImageType(1L, ImageType.MAIN))
                    .thenReturn(new ArrayList<>(List.of(testMainImage)));
            when(productImageRepository.save(any(ProductImage.class))).thenAnswer(inv -> {
                ProductImage img = inv.getArgument(0);
                if (img.getImageId() == null) {
                    img.setImageId(102L);
                }
                return img;
            });

            ProductImageResponse result = productImageService.addImage(1L, request);

            assertThat(result).isNotNull();
            assertThat(result.getImageId()).isEqualTo(102L);
            assertThat(result.getImageType()).isEqualTo("main");
            assertThat(testMainImage.getImageType()).isEqualTo(ImageType.SUB);
        }

        @Test
        @DisplayName("addImage with variant should throw IllegalArgumentException if variant belongs to different product")
        void addImage_variantMismatch_shouldThrow() {
            Product differentProduct = Product.builder().productId(2L).build();
            ProductVariant differentVariant = ProductVariant.builder().variantId(20L).product(differentProduct).build();

            ProductImageRequest request = ProductImageRequest.builder()
                    .variantId(20L)
                    .imageUrl("https://example.com/img.png")
                    .build();

            when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
            when(productVariantRepository.findById(20L)).thenReturn(Optional.of(differentVariant));

            assertThatThrownBy(() -> productImageService.addImage(1L, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("does not belong to product with id 1");
        }

        @Test
        @DisplayName("setMainImage should promote target image and demote previous main")
        void setMainImage_success() {
            when(productImageRepository.findById(101L)).thenReturn(Optional.of(testSubImage));
            when(productImageRepository.findByProduct_ProductIdAndVariantIsNullAndImageType(1L, ImageType.MAIN))
                    .thenReturn(new ArrayList<>(List.of(testMainImage)));
            when(productImageRepository.save(any(ProductImage.class))).thenAnswer(inv -> inv.getArgument(0));

            ProductImageResponse result = productImageService.setMainImage(101L);

            assertThat(result.getImageType()).isEqualTo("main");
            assertThat(testMainImage.getImageType()).isEqualTo(ImageType.SUB);
        }
    }

    @Nested
    @DisplayName("Update, Delete & Reorder Operations")
    class OtherTests {

        @Test
        @DisplayName("updateImage should update altText and imageUrl")
        void updateImage_success() {
            ProductImageRequest request = ProductImageRequest.builder()
                    .imageUrl("https://example.com/rtx4090-updated.png")
                    .altText("Updated Alt")
                    .sortOrder(5)
                    .build();

            when(productImageRepository.findById(101L)).thenReturn(Optional.of(testSubImage));
            when(productImageRepository.save(any(ProductImage.class))).thenReturn(testSubImage);

            ProductImageResponse updated = productImageService.updateImage(101L, request);

            assertThat(updated).isNotNull();
            assertThat(testSubImage.getImageUrl()).isEqualTo("https://example.com/rtx4090-updated.png");
            assertThat(testSubImage.getAltText()).isEqualTo("Updated Alt");
        }

        @Test
        @DisplayName("deleteImage should delete when found")
        void deleteImage_success() {
            when(productImageRepository.findById(100L)).thenReturn(Optional.of(testMainImage));

            productImageService.deleteImage(100L);

            verify(productImageRepository).delete(testMainImage);
        }

        @Test
        @DisplayName("updateSortOrders should reorder images")
        void updateSortOrders_success() {
            when(productImageRepository.findById(100L)).thenReturn(Optional.of(testMainImage));
            when(productImageRepository.findById(101L)).thenReturn(Optional.of(testSubImage));

            productImageService.updateSortOrders(1L, List.of(101L, 100L));

            assertThat(testSubImage.getSortOrder()).isEqualTo(0);
            assertThat(testMainImage.getSortOrder()).isEqualTo(1);
        }
    }
}
