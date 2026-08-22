package com.store.service.impl;

import com.store.dto.request.CategoryRequest;
import com.store.dto.response.CategoryResponse;
import com.store.dto.response.PageResponse;
import com.store.entity.category.Category;
import com.store.entity.category.CategoryStatus;
import com.store.exception.DuplicateResourceException;
import com.store.exception.ResourceNotFoundException;
import com.store.repository.CategoryRepository;
import com.store.service.CategoryService;
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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "categories", key = "'all'")
    public List<CategoryResponse> getAllCategories() {
        log.info("Fetching all categories from database (cache miss)");
        return categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "sortOrder", "name"))
                .stream()
                .map(CategoryResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "categories", key = "'tree'")
    public List<CategoryResponse> getCategoryTree() {
        log.info("Building category tree from database (cache miss)");
        List<Category> allCategories = categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "sortOrder", "name"));

        Map<Integer, CategoryResponse> lookup = new LinkedHashMap<>();
        List<CategoryResponse> rootCategories = new ArrayList<>();

        for (Category category : allCategories) {
            lookup.put(category.getCategoryId(), CategoryResponse.fromEntity(category));
        }

        for (Category category : allCategories) {
            CategoryResponse currentDto = lookup.get(category.getCategoryId());
            if (category.getParent() == null) {
                rootCategories.add(currentDto);
            } else {
                CategoryResponse parentDto = lookup.get(category.getParent().getCategoryId());
                if (parentDto != null) {
                    if (parentDto.getChildren() == null) {
                        parentDto.setChildren(new ArrayList<>());
                    }
                    parentDto.getChildren().add(currentDto);
                } else {
                    rootCategories.add(currentDto);
                }
            }
        }

        return rootCategories;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "categories", key = "'roots'")
    public List<CategoryResponse> getRootCategories() {
        log.info("Fetching root categories from database (cache miss)");
        return categoryRepository.findByParentIsNullOrderBySortOrderAscNameAsc()
                .stream()
                .map(CategoryResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "categories", key = "'children_' + #parentId")
    public List<CategoryResponse> getChildrenByParentId(Integer parentId) {
        log.info("Fetching direct children for parent category id {} from database (cache miss)", parentId);
        if (!categoryRepository.existsById(parentId)) {
            throw new ResourceNotFoundException("Parent category not found with id: " + parentId);
        }
        return categoryRepository.findByParent_CategoryIdOrderBySortOrderAscNameAsc(parentId)
                .stream()
                .map(CategoryResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CategoryResponse> getCategoriesPaginated(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<CategoryResponse> categoryPage = categoryRepository.findAll(pageable)
                .map(CategoryResponse::fromEntity);

        return PageResponse.of(categoryPage);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "categories", key = "#id")
    public CategoryResponse getCategoryById(Integer id) {
        log.info("Fetching category with id {} from database (cache miss)", id);
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
        return CategoryResponse.fromEntity(category);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "categories", key = "'slug_' + #slug")
    public CategoryResponse getCategoryBySlug(String slug) {
        log.info("Fetching category with slug {} from database (cache miss)", slug);
        Category category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with slug: " + slug));
        return CategoryResponse.fromEntity(category);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "categories", allEntries = true)
    public CategoryResponse createCategory(CategoryRequest request) {
        log.info("Creating category: {}", request.getName());
        String name = request.getName().trim();

        Category parent = null;
        if (request.getParentId() != null) {
            parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category not found with id: " + request.getParentId()));

            if (categoryRepository.existsByNameAndParent_CategoryId(name, request.getParentId())) {
                throw new DuplicateResourceException("Category with name '" + name + "' already exists under parent id " + request.getParentId());
            }
        } else {
            if (categoryRepository.existsByNameAndParentIsNull(name)) {
                throw new DuplicateResourceException("Root category with name '" + name + "' already exists");
            }
        }

        String slug = (request.getSlug() != null && !request.getSlug().isBlank())
                ? SlugUtils.toSlug(request.getSlug())
                : SlugUtils.toSlug(name);

        if (categoryRepository.existsBySlug(slug)) {
            throw new DuplicateResourceException("Category already exists with slug: " + slug);
        }

        CategoryStatus status = request.getStatus() != null
                ? CategoryStatus.fromValue(request.getStatus())
                : CategoryStatus.ACTIVE;

        Category category = Category.builder()
                .name(name)
                .slug(slug)
                .parent(parent)
                .iconUrl(request.getIconUrl())
                .description(request.getDescription())
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .status(status)
                .build();

        Category savedCategory = categoryRepository.save(category);
        return CategoryResponse.fromEntity(savedCategory);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "categories", allEntries = true)
    public CategoryResponse updateCategory(Integer id, CategoryRequest request) {
        log.info("Updating category id {}: {}", id, request.getName());
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        String name = request.getName().trim();
        Integer newParentId = request.getParentId();

        Category parent = null;
        if (newParentId != null) {
            if (newParentId.equals(id)) {
                throw new IllegalArgumentException("A category cannot be its own parent.");
            }

            Category targetParent = categoryRepository.findById(newParentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category not found with id: " + newParentId));

            // Circular hierarchy check: walk up from targetParent to root
            Category current = targetParent;
            while (current != null) {
                if (current.getCategoryId().equals(id)) {
                    throw new IllegalArgumentException("Cannot set parent category: Category ID " + id
                            + " is an ancestor of the selected parent ID " + newParentId + " (Circular reference detected).");
                }
                current = current.getParent();
            }

            if (categoryRepository.existsByNameAndParent_CategoryIdAndCategoryIdNot(name, newParentId, id)) {
                throw new DuplicateResourceException("Category with name '" + name + "' already exists under parent id " + newParentId);
            }
            parent = targetParent;
        } else {
            if (categoryRepository.existsByNameAndParentIsNullAndCategoryIdNot(name, id)) {
                throw new DuplicateResourceException("Root category with name '" + name + "' already exists");
            }
        }

        String slug = (request.getSlug() != null && !request.getSlug().isBlank())
                ? SlugUtils.toSlug(request.getSlug())
                : SlugUtils.toSlug(name);

        if (categoryRepository.existsBySlugAndCategoryIdNot(slug, id)) {
            throw new DuplicateResourceException("Category already exists with slug: " + slug);
        }

        CategoryStatus status = request.getStatus() != null
                ? CategoryStatus.fromValue(request.getStatus())
                : category.getStatus();

        category.setName(name);
        category.setSlug(slug);
        category.setParent(parent);
        category.setIconUrl(request.getIconUrl());
        category.setDescription(request.getDescription());
        if (request.getSortOrder() != null) {
            category.setSortOrder(request.getSortOrder());
        }
        category.setStatus(status);

        Category updatedCategory = categoryRepository.save(category);
        return CategoryResponse.fromEntity(updatedCategory);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "categories", allEntries = true)
    public void deleteCategory(Integer id) {
        log.info("Deleting category with id {}", id);
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
        categoryRepository.delete(category);
    }

    @Override
    @Transactional(readOnly = true)
    public long countChildren(Integer parentId) {
        return categoryRepository.countByParent_CategoryId(parentId);
    }
}
