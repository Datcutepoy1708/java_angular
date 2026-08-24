package com.store.service;

import com.store.dto.request.ProductFilterRequest;
import com.store.dto.request.ProductRequest;
import com.store.dto.response.PageResponse;
import com.store.dto.response.ProductResponse;
import com.store.entity.brand.Brand;
import com.store.entity.category.Category;
import com.store.entity.category.CategoryStatus;
import com.store.entity.product.Product;
import com.store.entity.product.ProductStatus;
import com.store.entity.supplier.Supplier;
import com.store.exception.DuplicateResourceException;
import com.store.exception.ResourceNotFoundException;
import com.store.repository.BrandRepository;
import com.store.repository.CategoryRepository;
import com.store.repository.ProductRepository;
import com.store.repository.SupplierRepository;
import com.store.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private BrandRepository brandRepository;

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private com.store.repository.ProductAttributeValueRepository productAttributeValueRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    private Category testCategory;
    private Brand testBrand;
    private Supplier testSupplier;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        testCategory = Category.builder()
                .categoryId(1)
                .name("CPU - Bộ vi xử lý")
                .slug("cpu-bo-vi-xu-ly")
                .status(CategoryStatus.ACTIVE)
                .build();

        testBrand = Brand.builder()
                .brandId(1)
                .name("Intel")
                .slug("intel")
                .build();

        testSupplier = Supplier.builder()
                .supplierId(1)
                .name("Công ty Phân Phối Synnex FPT")
                .build();

        testProduct = Product.builder()
                .productId(100L)
                .category(testCategory)
                .brand(testBrand)
                .supplier(testSupplier)
                .name("Intel Core i9 14900K")
                .slug("intel-core-i9-14900k")
                .sku("CPU-INTEL-14900K")
                .shortDesc("Vi xử lý flagship 24 nhân 32 luồng")
                .description("Chi tiết CPU...")
                .warrantyMonths(36)
                .status(ProductStatus.ACTIVE)
                .viewCount(50)
                .build();
    }

    @Nested
    @DisplayName("Read Operations")
    class ReadTests {

        @Test
        @DisplayName("getProducts should return paginated list with filter criteria")
        void getProducts_success() {
            Page<Product> page = new PageImpl<>(List.of(testProduct));
            when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

            ProductFilterRequest filter = ProductFilterRequest.builder()
                    .categoryId(1)
                    .includeChildren(false)
                    .keyword("i9")
                    .page(0)
                    .size(10)
                    .build();

            PageResponse<ProductResponse> result = productService.getProducts(filter);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getName()).isEqualTo("Intel Core i9 14900K");
            assertThat(result.getContent().get(0).getCategoryName()).isEqualTo("CPU - Bộ vi xử lý");
            assertThat(result.getContent().get(0).getBrandName()).isEqualTo("Intel");
        }

        @Test
        @DisplayName("getProductById should return product response when found")
        void getProductById_found() {
            when(productRepository.findById(100L)).thenReturn(Optional.of(testProduct));

            ProductResponse response = productService.getProductById(100L);

            assertThat(response).isNotNull();
            assertThat(response.getProductId()).isEqualTo(100L);
            assertThat(response.getName()).isEqualTo("Intel Core i9 14900K");
            assertThat(response.getWarrantyMonths()).isEqualTo(36);
        }

        @Test
        @DisplayName("getProductById should throw ResourceNotFoundException when not found")
        void getProductById_notFound() {
            when(productRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.getProductById(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Product not found with id: 999");
        }

        @Test
        @DisplayName("getProductBySlug should return product response when found")
        void getProductBySlug_found() {
            when(productRepository.findBySlug("intel-core-i9-14900k")).thenReturn(Optional.of(testProduct));

            ProductResponse response = productService.getProductBySlug("intel-core-i9-14900k");

            assertThat(response).isNotNull();
            assertThat(response.getSlug()).isEqualTo("intel-core-i9-14900k");
        }
    }

    @Nested
    @DisplayName("Create Operations")
    class CreateTests {

        @Test
        @DisplayName("createProduct should succeed when valid")
        void createProduct_success() {
            ProductRequest request = ProductRequest.builder()
                    .categoryId(1)
                    .brandId(1)
                    .supplierId(1)
                    .name("Intel Core i7 14700K")
                    .slug("intel-core-i7-14700k")
                    .sku("CPU-INTEL-14700K")
                    .warrantyMonths(36)
                    .status("active")
                    .build();

            when(categoryRepository.findById(1)).thenReturn(Optional.of(testCategory));
            when(brandRepository.findById(1)).thenReturn(Optional.of(testBrand));
            when(supplierRepository.findById(1)).thenReturn(Optional.of(testSupplier));
            when(productRepository.existsBySlug("intel-core-i7-14700k")).thenReturn(false);
            when(productRepository.existsBySku("CPU-INTEL-14700K")).thenReturn(false);
            when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
                Product p = inv.getArgument(0);
                p.setProductId(101L);
                return p;
            });

            ProductResponse created = productService.createProduct(request);

            assertThat(created).isNotNull();
            assertThat(created.getProductId()).isEqualTo(101L);
            assertThat(created.getName()).isEqualTo("Intel Core i7 14700K");
            assertThat(created.getStatus()).isEqualTo("active");
        }

        @Test
        @DisplayName("createProduct when category does not exist should throw ResourceNotFoundException")
        void createProduct_categoryNotFound() {
            ProductRequest request = ProductRequest.builder()
                    .categoryId(999)
                    .name("Intel Core i7")
                    .build();

            when(categoryRepository.findById(999)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.createProduct(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Category not found with id: 999");
        }

        @Test
        @DisplayName("createProduct when duplicate slug should throw DuplicateResourceException")
        void createProduct_duplicateSlug() {
            ProductRequest request = ProductRequest.builder()
                    .categoryId(1)
                    .name("Intel Core i9 14900K")
                    .slug("intel-core-i9-14900k")
                    .build();

            when(categoryRepository.findById(1)).thenReturn(Optional.of(testCategory));
            when(productRepository.existsBySlug("intel-core-i9-14900k")).thenReturn(true);

            assertThatThrownBy(() -> productService.createProduct(request))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("Product already exists with slug: intel-core-i9-14900k");
        }
    }

    @Nested
    @DisplayName("Update Operations")
    class UpdateTests {

        @Test
        @DisplayName("updateProduct should succeed when valid")
        void updateProduct_success() {
            ProductRequest request = ProductRequest.builder()
                    .categoryId(1)
                    .brandId(1)
                    .name("Intel Core i9 14900KS")
                    .slug("intel-core-i9-14900ks")
                    .sku("CPU-INTEL-14900KS")
                    .status("discontinued")
                    .build();

            when(productRepository.findById(100L)).thenReturn(Optional.of(testProduct));
            when(categoryRepository.findById(1)).thenReturn(Optional.of(testCategory));
            when(brandRepository.findById(1)).thenReturn(Optional.of(testBrand));
            when(productRepository.existsBySlugAndProductIdNot("intel-core-i9-14900ks", 100L)).thenReturn(false);
            when(productRepository.existsBySkuAndProductIdNot("CPU-INTEL-14900KS", 100L)).thenReturn(false);
            when(productRepository.save(any(Product.class))).thenReturn(testProduct);

            ProductResponse updated = productService.updateProduct(100L, request);

            assertThat(updated).isNotNull();
            assertThat(testProduct.getName()).isEqualTo("Intel Core i9 14900KS");
            assertThat(testProduct.getStatus()).isEqualTo(ProductStatus.DISCONTINUED);
        }
    }

    @Nested
    @DisplayName("Delete and Other Operations")
    class OtherTests {

        @Test
        @DisplayName("deleteProduct should soft-delete when found")
        void deleteProduct_success() {
            when(productRepository.findById(100L)).thenReturn(Optional.of(testProduct));

            productService.deleteProduct(100L);

            verify(productRepository).save(testProduct);
            assertThat(testProduct.getDeletedAt()).isNotNull();
        }

        @Test
        @DisplayName("deleteProduct should throw ResourceNotFoundException when not found")
        void deleteProduct_notFound() {
            when(productRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.deleteProduct(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Product not found with id: 999");

            verify(productRepository, never()).save(any(Product.class));
        }

        @Test
        @DisplayName("incrementViewCount should increment view count")
        void incrementViewCount_success() {
            when(productRepository.findById(100L)).thenReturn(Optional.of(testProduct));

            productService.incrementViewCount(100L);

            assertThat(testProduct.getViewCount()).isEqualTo(51);
            verify(productRepository).save(testProduct);
        }
    }
}
