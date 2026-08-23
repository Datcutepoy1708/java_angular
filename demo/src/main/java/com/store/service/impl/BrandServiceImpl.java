package com.store.service.impl;

import com.store.dto.request.BrandRequest;
import com.store.dto.request.BulkActionRequest;
import com.store.dto.response.BrandResponse;
import com.store.dto.response.BulkActionResult;
import com.store.dto.response.PageResponse;
import com.store.entity.brand.Brand;
import com.store.entity.brand.BrandStatus;
import com.store.exception.DuplicateResourceException;
import com.store.exception.ResourceNotFoundException;
import com.store.repository.BrandRepository;
import com.store.service.BrandService;
import com.store.util.SlugUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BrandServiceImpl implements BrandService {

    private final BrandRepository brandRepository;

    // ── Read ──────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "brands", key = "'all'")
    public List<BrandResponse> getAllBrands() {
        log.info("Fetching all active brands from database (cache miss)");
        return brandRepository.findAllActive()
                .stream()
                .map(BrandResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BrandResponse> getBrandsPaginated(
            int page, int size, String keyword, String status,
            String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        String kw = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        String st = (status != null && !status.isBlank()) ? status.trim() : null;

        Page<BrandResponse> brandPage = brandRepository
                .findAllActiveFiltered(kw, st, pageable)
                .map(BrandResponse::fromEntity);
        return PageResponse.of(brandPage);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BrandResponse> getDeletedBrands(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("deletedAt").descending());
        Page<BrandResponse> result = brandRepository.findByDeletedAtIsNotNull(pageable)
                .map(BrandResponse::fromEntity);
        return PageResponse.of(result);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "brands", key = "#id")
    public BrandResponse getBrandById(Integer id) {
        log.info("Fetching brand with id {} from database (cache miss)", id);
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with id: " + id));
        return BrandResponse.fromEntity(brand);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "brands", key = "'slug_' + #slug")
    public BrandResponse getBrandBySlug(String slug) {
        log.info("Fetching brand with slug {} from database (cache miss)", slug);
        Brand brand = brandRepository.findBySlugAndDeletedAtIsNull(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with slug: " + slug));
        return BrandResponse.fromEntity(brand);
    }

    // ── Write ─────────────────────────────────────────────────────

    @Override
    @Transactional
    @CacheEvict(cacheNames = "brands", allEntries = true)
    public BrandResponse createBrand(BrandRequest request) {
        log.info("Creating brand: {}", request.getName());
        String name = request.getName().trim();
        if (brandRepository.existsByNameAndDeletedAtIsNull(name)) {
            throw new DuplicateResourceException("Brand already exists with name: " + name);
        }

        String slug = (request.getSlug() != null && !request.getSlug().isBlank())
                ? SlugUtils.toSlug(request.getSlug())
                : SlugUtils.toSlug(name);

        if (brandRepository.existsBySlug(slug)) {
            throw new DuplicateResourceException("Brand already exists with slug: " + slug);
        }

        Brand brand = Brand.builder()
                .name(name)
                .slug(slug)
                .logoUrl(request.getLogoUrl())
                .country(request.getCountry())
                .description(request.getDescription())
                .status(BrandStatus.ACTIVE)
                .build();

        return BrandResponse.fromEntity(brandRepository.save(brand));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "brands", allEntries = true)
    public BrandResponse updateBrand(Integer id, BrandRequest request) {
        log.info("Updating brand id {}: {}", id, request.getName());
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with id: " + id));

        String name = request.getName().trim();
        if (brandRepository.existsByNameAndBrandIdNot(name, id)) {
            throw new DuplicateResourceException("Brand already exists with name: " + name);
        }

        String slug = (request.getSlug() != null && !request.getSlug().isBlank())
                ? SlugUtils.toSlug(request.getSlug())
                : SlugUtils.toSlug(name);

        if (brandRepository.existsBySlugAndBrandIdNot(slug, id)) {
            throw new DuplicateResourceException("Brand already exists with slug: " + slug);
        }

        brand.setName(name);
        brand.setSlug(slug);
        brand.setLogoUrl(request.getLogoUrl());
        brand.setCountry(request.getCountry());
        brand.setDescription(request.getDescription());

        return BrandResponse.fromEntity(brandRepository.save(brand));
    }

    // ── Soft-delete / Restore ─────────────────────────────────────

    @Override
    @Transactional
    @CacheEvict(cacheNames = "brands", allEntries = true)
    public void softDeleteBrand(Integer id) {
        log.info("Soft-deleting brand with id {}", id);
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with id: " + id));
        if (brand.getDeletedAt() != null) {
            throw new IllegalStateException("Brand is already deleted");
        }
        // Only set deletedAt — status is intentionally NOT changed
        brand.setDeletedAt(LocalDateTime.now());
        brandRepository.save(brand);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "brands", allEntries = true)
    public void restoreBrand(Integer id) {
        log.info("Restoring brand with id {}", id);
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with id: " + id));
        if (brand.getDeletedAt() == null) {
            throw new IllegalStateException("Brand is not deleted");
        }
        // Only clear deletedAt — status returns to its original value automatically
        brand.setDeletedAt(null);
        brandRepository.save(brand);
    }

    // ── Bulk Action ───────────────────────────────────────────────

    @Override
    @Transactional
    @CacheEvict(cacheNames = "brands", allEntries = true)
    public BulkActionResult bulkAction(BulkActionRequest request) {
        log.info("Bulk action '{}' on {} brand(s)", request.getAction(), request.getIds().size());
        List<BulkActionResult.BulkItemResult> results = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;

        for (Long id : request.getIds()) {
            try {
                Brand brand = brandRepository.findById(id.intValue())
                        .orElseThrow(() -> new ResourceNotFoundException("NOT_FOUND"));

                switch (request.getAction()) {
                    case "delete" -> {
                        if (brand.getDeletedAt() != null) throw new IllegalStateException("ALREADY_DELETED");
                        brand.setDeletedAt(LocalDateTime.now());
                    }
                    case "restore" -> {
                        if (brand.getDeletedAt() == null) throw new IllegalStateException("ALREADY_ACTIVE");
                        brand.setDeletedAt(null);
                    }
                    case "activate" -> brand.setStatus(BrandStatus.ACTIVE);
                    case "deactivate" -> brand.setStatus(BrandStatus.INACTIVE);
                    default -> throw new IllegalArgumentException("Unknown action: " + request.getAction());
                }
                brandRepository.save(brand);
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

    /** @deprecated Use softDeleteBrand instead */
    @Override
    @Deprecated
    @Transactional
    @CacheEvict(cacheNames = "brands", allEntries = true)
    public void deleteBrand(Integer id) {
        softDeleteBrand(id);
    }
}
