package com.store.service.impl;

import com.store.dto.request.BulkActionRequest;
import com.store.dto.request.CategoryRequest;
import com.store.dto.response.BulkActionResult;
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

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    // ── Read ──────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "categories", key = "'all'")
    public List<CategoryResponse> getAllCategories() {
        log.info("Fetching all active categories from database (cache miss)");
        return categoryRepository.findAllActive()
                .stream()
                .map(CategoryResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "categories", key = "'tree'")
    public List<CategoryResponse> getCategoryTree() {
        log.info("Building category tree (active only) from database (cache miss)");
        List<Category> allActive = categoryRepository.findAllActive();

        Map<Integer, CategoryResponse> lookup = new LinkedHashMap<>();
        List<CategoryResponse> rootCategories = new ArrayList<>();

        for (Category category : allActive) {
            lookup.put(category.getCategoryId(), CategoryResponse.fromEntity(category));
        }

        for (Category category : allActive) {
            CategoryResponse currentDto = lookup.get(category.getCategoryId());
            if (category.getParent() == null || category.getParent().getDeletedAt() != null) {
                // Root or parent has been soft-deleted — treat as root in the tree
                rootCategories.add(currentDto);
            } else {
                CategoryResponse parentDto = lookup.get(category.getParent().getCategoryId());
                if (parentDto != null) {
                    if (parentDto.getChildren() == null) {
                        parentDto.setChildren(new ArrayList<>());
                    }
                    parentDto.getChildren().add(currentDto);
                } else {
                    // Parent filtered out (soft-deleted) — promote to root
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
        log.info("Fetching active root categories from database (cache miss)");
        return categoryRepository.findActiveRoots()
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
                .filter(c -> c.getDeletedAt() == null) // only active children
                .map(CategoryResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CategoryResponse> getCategoriesPaginated(int page, int size, String keyword, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        String kw = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        Page<CategoryResponse> result = categoryRepository.findAllActiveFiltered(kw, pageable)
                .map(CategoryResponse::fromEntity);
        return PageResponse.of(result);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CategoryResponse> getDeletedCategories(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("deletedAt").descending());
        Page<CategoryResponse> result = categoryRepository.findByDeletedAtIsNotNull(pageable)
                .map(CategoryResponse::fromEntity);
        return PageResponse.of(result);
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
        Category category = categoryRepository.findBySlugActive(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with slug: " + slug));
        return CategoryResponse.fromEntity(category);
    }

    // ── Write ─────────────────────────────────────────────────────

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

        return CategoryResponse.fromEntity(categoryRepository.save(category));
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

            Category current = targetParent;
            while (current != null) {
                if (current.getCategoryId().equals(id)) {
                    throw new IllegalArgumentException("Cannot set parent category: circular reference detected.");
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

        return CategoryResponse.fromEntity(categoryRepository.save(category));
    }

    // ── Soft-delete / Restore (recursive) ────────────────────────

    @Override
    @Transactional
    @CacheEvict(cacheNames = "categories", allEntries = true)
    public void softDeleteCategory(Integer id) {
        log.info("Soft-deleting category id {} and all descendants", id);
        Category root = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
        if (root.getDeletedAt() != null) {
            throw new IllegalStateException("Category is already deleted");
        }

        List<Category> toDelete = collectSubtree(id);
        LocalDateTime now = LocalDateTime.now();
        for (Category cat : toDelete) {
            if (cat.getDeletedAt() == null) { // skip already deleted
                cat.setDeletedAt(now);
            }
        }
        categoryRepository.saveAll(toDelete);
        log.info("Soft-deleted {} categories (including descendants)", toDelete.size());
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "categories", allEntries = true)
    public void restoreCategory(Integer id) {
        log.info("Restoring category id {} and all descendants", id);
        Category root = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
        if (root.getDeletedAt() == null) {
            throw new IllegalStateException("Category is not deleted");
        }

        // Restore all descendants with deleted_at IS NOT NULL in the subtree
        // Note: this intentionally restores descendants that were independently deleted before the parent,
        // as documented in the accepted limitation (accepted per design decision, Aug 2026).
        List<Category> toRestore = collectSubtree(id);
        for (Category cat : toRestore) {
            cat.setDeletedAt(null);
        }
        categoryRepository.saveAll(toRestore);
        log.info("Restored {} categories (including descendants)", toRestore.size());
    }

    /**
     * Collects the root category and ALL descendants recursively using in-memory traversal.
     * Loads all categories (including deleted) to handle multi-level trees correctly.
     */
    private List<Category> collectSubtree(Integer rootId) {
        // Load all categories into memory for efficient in-memory tree traversal
        List<Category> all = categoryRepository.findAllIncludingDeleted();
        Map<Integer, List<Category>> childrenByParentId = all.stream()
                .filter(c -> c.getParent() != null)
                .collect(Collectors.groupingBy(c -> c.getParent().getCategoryId()));

        Map<Integer, Category> byId = all.stream()
                .collect(Collectors.toMap(Category::getCategoryId, c -> c));

        List<Category> result = new ArrayList<>();
        Deque<Integer> queue = new ArrayDeque<>();
        queue.push(rootId);

        while (!queue.isEmpty()) {
            Integer currentId = queue.pop();
            Category current = byId.get(currentId);
            if (current != null) {
                result.add(current);
                List<Category> children = childrenByParentId.getOrDefault(currentId, List.of());
                for (Category child : children) {
                    queue.push(child.getCategoryId());
                }
            }
        }
        return result;
    }

    // ── Bulk Action ───────────────────────────────────────────────

    @Override
    @Transactional
    @CacheEvict(cacheNames = "categories", allEntries = true)
    public BulkActionResult bulkAction(BulkActionRequest request) {
        log.info("Bulk action '{}' on {} categories", request.getAction(), request.getIds().size());
        List<BulkActionResult.BulkItemResult> results = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;

        for (Long id : request.getIds()) {
            try {
                switch (request.getAction()) {
                    case "delete" -> softDeleteCategory(id.intValue());
                    case "restore" -> restoreCategory(id.intValue());
                    default -> throw new IllegalArgumentException("Unknown action: " + request.getAction());
                }
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
    @Transactional(readOnly = true)
    public long countChildren(Integer parentId) {
        return categoryRepository.countByParent_CategoryId(parentId);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Set<Integer> getCategoryAndDescendantIds(Integer rootId) {
        if (rootId == null) return java.util.Collections.emptySet();
        List<Category> subtree = collectSubtree(rootId);
        java.util.Set<Integer> ids = new java.util.HashSet<>();
        for (Category c : subtree) {
            ids.add(c.getCategoryId());
        }
        return ids;
    }

    /** @deprecated Use softDeleteCategory instead */
    @Override
    @Deprecated
    @Transactional
    @CacheEvict(cacheNames = "categories", allEntries = true)
    public void deleteCategory(Integer id) {
        softDeleteCategory(id);
    }
}
