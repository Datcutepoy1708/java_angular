package com.store.cache;

import com.store.dto.request.BrandRequest;
import com.store.dto.request.CategoryRequest;
import com.store.dto.response.BrandResponse;
import com.store.dto.response.CategoryResponse;
import com.store.service.BrandService;
import com.store.service.CategoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "jwt.secret=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970")
class CacheEvictionIntegrationTest {

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private BrandService brandService;

    @Autowired
    private CategoryService categoryService;

    @Test
    @DisplayName("Brand cache should cache on read and evict all entries on update")
    void brandCache_readAndEvict_success() {
        // 1. Create a brand
        BrandRequest createRequest = BrandRequest.builder()
                .name("Brand Cache Test")
                .slug("brand-cache-test-" + System.currentTimeMillis())
                .description("Testing cache")
                .build();
        BrandResponse created = brandService.createBrand(createRequest);
        Integer brandId = created.getBrandId();

        // 2. Read brand by id (triggers @Cacheable(cacheNames = "brands"))
        BrandResponse firstRead = brandService.getBrandById(brandId);
        assertThat(firstRead).isNotNull();

        Cache brandsCache = cacheManager.getCache("brands");
        assertThat(brandsCache).isNotNull();

        // Verify cache entry exists
        Cache.ValueWrapper cachedValue = brandsCache.get(brandId);
        assertThat(cachedValue).isNotNull();

        // 3. Update brand (triggers @CacheEvict(cacheNames = "brands", allEntries = true))
        BrandRequest updateRequest = BrandRequest.builder()
                .name("Brand Cache Test Updated")
                .slug(created.getSlug())
                .description("Updated description")
                .build();
        brandService.updateBrand(brandId, updateRequest);

        // 4. Verify cache entry has been evicted
        Cache.ValueWrapper afterEvict = brandsCache.get(brandId);
        assertThat(afterEvict).isNull();

        // Cleanup
        brandService.deleteBrand(brandId);
    }

    @Test
    @DisplayName("Category cache should cache tree on read and evict on update")
    void categoryCache_readAndEvict_success() {
        // 1. Create a category
        CategoryRequest request = CategoryRequest.builder()
                .name("Category Cache Test")
                .slug("category-cache-test-" + System.currentTimeMillis())
                .status("active")
                .build();
        CategoryResponse created = categoryService.createCategory(request);
        Integer catId = created.getCategoryId();

        // 2. Read category by id (triggers @Cacheable(cacheNames = "categories"))
        CategoryResponse firstRead = categoryService.getCategoryById(catId);
        assertThat(firstRead).isNotNull();

        Cache categoriesCache = cacheManager.getCache("categories");
        assertThat(categoriesCache).isNotNull();

        // Verify cache entry exists
        Cache.ValueWrapper cachedValue = categoriesCache.get(catId);
        assertThat(cachedValue).isNotNull();

        // 3. Update category (triggers @CacheEvict(cacheNames = "categories", allEntries = true))
        CategoryRequest updateRequest = CategoryRequest.builder()
                .name("Category Cache Test Updated")
                .slug(created.getSlug())
                .status("active")
                .build();
        categoryService.updateCategory(catId, updateRequest);

        // 4. Verify cache entry has been evicted
        Cache.ValueWrapper afterEvict = categoriesCache.get(catId);
        assertThat(afterEvict).isNull();

        // Cleanup
        categoryService.deleteCategory(catId);
    }
}
