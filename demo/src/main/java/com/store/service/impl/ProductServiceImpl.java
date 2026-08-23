package com.store.service.impl;

import com.store.dto.request.BulkActionRequest;
import com.store.dto.request.ProductFilterRequest;
import com.store.dto.request.ProductRequest;
import com.store.dto.response.BulkActionResult;
import com.store.dto.response.PageResponse;
import com.store.dto.response.ProductResponse;
import com.store.entity.brand.Brand;
import com.store.entity.category.Category;
import com.store.entity.product.Product;
import com.store.entity.product.ProductStatus;
import com.store.entity.supplier.Supplier;
import com.store.exception.DuplicateResourceException;
import com.store.exception.ResourceNotFoundException;
import com.store.repository.BrandRepository;
import com.store.repository.CategoryRepository;
import com.store.repository.ProductRepository;
import com.store.repository.ProductSpecification;
import com.store.repository.SupplierRepository;
import com.store.service.ProductService;
import com.store.util.SlugUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final SupplierRepository supplierRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getProducts(ProductFilterRequest filter) {
        log.info("Fetching products with filter: {}", filter);

        List<Integer> categoryIds = null;
        if (filter.getCategoryId() != null) {
            categoryIds = new ArrayList<>();
            categoryIds.add(filter.getCategoryId());

            if (Boolean.TRUE.equals(filter.getIncludeChildren())) {
                List<Category> children = categoryRepository.findByParent_CategoryIdOrderBySortOrderAscNameAsc(filter.getCategoryId());
                for (Category child : children) {
                    categoryIds.add(child.getCategoryId());
                }
            }
        }

        ProductStatus productStatus = null;
        if (filter.getStatus() != null && !filter.getStatus().isBlank()) {
            productStatus = ProductStatus.fromValue(filter.getStatus());
        }

        Specification<Product> spec = ProductSpecification.filterBy(
                filter.getCategoryId(),
                categoryIds,
                filter.getBrandId(),
                filter.getSupplierId(),
                productStatus,
                filter.getKeyword()
        );

        String sortBy = (filter.getSortBy() != null && !filter.getSortBy().isBlank()) ? filter.getSortBy() : "createdAt";
        String sortDir = (filter.getSortDir() != null && !filter.getSortDir().isBlank()) ? filter.getSortDir() : "desc";
        int page = filter.getPage() != null && filter.getPage() >= 0 ? filter.getPage() : 0;
        int size = filter.getSize() != null && filter.getSize() > 0 ? filter.getSize() : 10;

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ProductResponse> productPage = productRepository.findAll(spec, pageable)
                .map(ProductResponse::fromEntity);

        return PageResponse.of(productPage);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getDeletedProducts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("deletedAt").descending());
        Page<ProductResponse> result = productRepository.findByDeletedAtIsNotNull(pageable)
                .map(ProductResponse::fromEntity);
        return PageResponse.of(result);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "productDetail", key = "#id")
    public ProductResponse getProductById(Long id) {
        log.info("Fetching product with id {} from database (cache miss)", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return ProductResponse.fromEntity(product);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "productDetail", key = "'slug_' + #slug")
    public ProductResponse getProductBySlug(String slug) {
        log.info("Fetching product with slug {} from database (cache miss)", slug);
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with slug: " + slug));
        return ProductResponse.fromEntity(product);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"products", "productDetail"}, allEntries = true)
    public ProductResponse createProduct(ProductRequest request) {
        log.info("Creating product: {}", request.getName());

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.getCategoryId()));

        Brand brand = null;
        if (request.getBrandId() != null) {
            brand = brandRepository.findById(request.getBrandId())
                    .orElseThrow(() -> new ResourceNotFoundException("Brand not found with id: " + request.getBrandId()));
        }

        Supplier supplier = null;
        if (request.getSupplierId() != null) {
            supplier = supplierRepository.findById(request.getSupplierId())
                    .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + request.getSupplierId()));
        }

        String name = request.getName().trim();
        String slug = (request.getSlug() != null && !request.getSlug().isBlank())
                ? SlugUtils.toSlug(request.getSlug())
                : SlugUtils.toSlug(name);

        if (productRepository.existsBySlug(slug)) {
            throw new DuplicateResourceException("Product already exists with slug: " + slug);
        }

        String sku = (request.getSku() != null && !request.getSku().isBlank())
                ? request.getSku().trim().toUpperCase()
                : "SKU-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        if (productRepository.existsBySku(sku)) {
            throw new DuplicateResourceException("Product already exists with SKU: " + sku);
        }

        ProductStatus status = request.getStatus() != null
                ? ProductStatus.fromValue(request.getStatus())
                : ProductStatus.ACTIVE;

        Product product = Product.builder()
                .category(category)
                .brand(brand)
                .supplier(supplier)
                .name(name)
                .slug(slug)
                .sku(sku)
                .shortDesc(request.getShortDesc())
                .description(request.getDescription())
                .warrantyMonths(request.getWarrantyMonths() != null ? request.getWarrantyMonths() : 12)
                .status(status)
                .viewCount(0)
                .build();

        Product savedProduct = productRepository.save(product);
        return ProductResponse.fromEntity(savedProduct);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"products", "productDetail"}, allEntries = true)
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        log.info("Updating product id {}: {}", id, request.getName());

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.getCategoryId()));

        Brand brand = null;
        if (request.getBrandId() != null) {
            brand = brandRepository.findById(request.getBrandId())
                    .orElseThrow(() -> new ResourceNotFoundException("Brand not found with id: " + request.getBrandId()));
        }

        Supplier supplier = null;
        if (request.getSupplierId() != null) {
            supplier = supplierRepository.findById(request.getSupplierId())
                    .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + request.getSupplierId()));
        }

        String name = request.getName().trim();
        String slug = (request.getSlug() != null && !request.getSlug().isBlank())
                ? SlugUtils.toSlug(request.getSlug())
                : SlugUtils.toSlug(name);

        if (productRepository.existsBySlugAndProductIdNot(slug, id)) {
            throw new DuplicateResourceException("Product already exists with slug: " + slug);
        }

        String sku = (request.getSku() != null && !request.getSku().isBlank())
                ? request.getSku().trim().toUpperCase()
                : product.getSku();

        if (sku != null && productRepository.existsBySkuAndProductIdNot(sku, id)) {
            throw new DuplicateResourceException("Product already exists with SKU: " + sku);
        }

        ProductStatus status = request.getStatus() != null
                ? ProductStatus.fromValue(request.getStatus())
                : product.getStatus();

        product.setCategory(category);
        product.setBrand(brand);
        product.setSupplier(supplier);
        product.setName(name);
        product.setSlug(slug);
        product.setSku(sku);
        product.setShortDesc(request.getShortDesc());
        product.setDescription(request.getDescription());
        if (request.getWarrantyMonths() != null) {
            product.setWarrantyMonths(request.getWarrantyMonths());
        }
        product.setStatus(status);

        Product updatedProduct = productRepository.save(product);
        return ProductResponse.fromEntity(updatedProduct);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"products", "productDetail"}, allEntries = true)
    public void softDeleteProduct(Long id) {
        log.info("Soft-deleting product with id {}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        if (product.getDeletedAt() != null) {
            throw new IllegalStateException("Product is already deleted");
        }
        // Only set deletedAt — status and variants/images are NOT changed
        product.setDeletedAt(java.time.LocalDateTime.now());
        productRepository.save(product);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"products", "productDetail"}, allEntries = true)
    public void restoreProduct(Long id) {
        log.info("Restoring product with id {}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        if (product.getDeletedAt() == null) {
            throw new IllegalStateException("Product is not deleted");
        }
        product.setDeletedAt(null);
        productRepository.save(product);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"products", "productDetail"}, allEntries = true)
    public BulkActionResult bulkAction(BulkActionRequest request) {
        log.info("Bulk action '{}' on {} product(s)", request.getAction(), request.getIds().size());
        List<BulkActionResult.BulkItemResult> results = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;

        for (Long id : request.getIds()) {
            try {
                Product product = productRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("NOT_FOUND"));

                switch (request.getAction()) {
                    case "delete" -> {
                        if (product.getDeletedAt() != null) throw new IllegalStateException("ALREADY_DELETED");
                        product.setDeletedAt(java.time.LocalDateTime.now());
                    }
                    case "restore" -> {
                        if (product.getDeletedAt() == null) throw new IllegalStateException("ALREADY_ACTIVE");
                        product.setDeletedAt(null);
                    }
                    default -> throw new IllegalArgumentException("Unknown action: " + request.getAction());
                }
                productRepository.save(product);
                results.add(BulkActionResult.BulkItemResult.builder().id(id).success(true).build());
                successCount++;
            } catch (Exception e) {
                results.add(BulkActionResult.BulkItemResult.builder()
                        .id(id).success(false).error(e.getMessage()).build());
                failCount++;
            }
        }

        return BulkActionResult.builder()
                .successCount(successCount)
                .failCount(failCount)
                .results(results)
                .build();
    }

    @Override
    @Transactional
    public void incrementViewCount(Long id) {
        log.info("Incrementing view count for product id {}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        product.setViewCount(product.getViewCount() + 1);
        productRepository.save(product);
    }

    /** @deprecated Use softDeleteProduct instead */
    @Override
    @Deprecated
    @Transactional
    @CacheEvict(cacheNames = {"products", "productDetail"}, allEntries = true)
    public void deleteProduct(Long id) {
        softDeleteProduct(id);
    }
}
