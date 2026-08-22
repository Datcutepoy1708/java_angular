package com.store.service.impl;

import com.store.dto.request.BrandRequest;
import com.store.dto.response.BrandResponse;
import com.store.dto.response.PageResponse;
import com.store.entity.brand.Brand;
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

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BrandServiceImpl implements BrandService {

    private final BrandRepository brandRepository;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "brands", key = "'all'")
    public List<BrandResponse> getAllBrands() {
        log.info("Fetching all brands from database (cache miss)");
        return brandRepository.findAll(Sort.by(Sort.Direction.ASC, "name"))
                .stream()
                .map(BrandResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BrandResponse> getBrandsPaginated(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<BrandResponse> brandPage = brandRepository.findAll(pageable)
                .map(BrandResponse::fromEntity);

        return PageResponse.of(brandPage);
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
        Brand brand = brandRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with slug: " + slug));
        return BrandResponse.fromEntity(brand);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "brands", allEntries = true)
    public BrandResponse createBrand(BrandRequest request) {
        log.info("Creating brand: {}", request.getName());
        String name = request.getName().trim();
        if (brandRepository.existsByName(name)) {
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
                .build();

        Brand savedBrand = brandRepository.save(brand);
        return BrandResponse.fromEntity(savedBrand);
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

        Brand updatedBrand = brandRepository.save(brand);
        return BrandResponse.fromEntity(updatedBrand);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "brands", allEntries = true)
    public void deleteBrand(Integer id) {
        log.info("Deleting brand with id {}", id);
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with id: " + id));
        brandRepository.delete(brand);
    }
}
