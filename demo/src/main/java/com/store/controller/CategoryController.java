package com.store.controller;

import com.store.dto.request.BulkActionRequest;
import com.store.dto.request.CategoryRequest;
import com.store.dto.response.ApiResponse;
import com.store.dto.response.BulkActionResult;
import com.store.dto.response.CategoryResponse;
import com.store.dto.response.PageResponse;
import com.store.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Category", description = "Category management APIs with parent-child hierarchy")
public class CategoryController {

    private final CategoryService categoryService;

    // ── Public / Read ──────────────────────────────────────────────

    @GetMapping
    @Operation(summary = "Get all active categories (Cached)")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllCategories() {
        return ResponseEntity.ok(ApiResponse.success("Categories retrieved successfully", categoryService.getAllCategories()));
    }

    @GetMapping("/tree")
    @Operation(summary = "Get active category tree (Cached)")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getCategoryTree() {
        return ResponseEntity.ok(ApiResponse.success("Category tree retrieved successfully", categoryService.getCategoryTree()));
    }

    @GetMapping("/roots")
    @Operation(summary = "Get active root categories (Cached)")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getRootCategories() {
        return ResponseEntity.ok(ApiResponse.success("Root categories retrieved successfully", categoryService.getRootCategories()));
    }

    @GetMapping("/{id}/children")
    @Operation(summary = "Get direct active children")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getChildren(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success("Children categories retrieved successfully", categoryService.getChildrenByParentId(id)));
    }

    @GetMapping("/{id}/children/count")
    @Operation(summary = "Count direct children")
    public ResponseEntity<ApiResponse<Map<String, Object>>> countChildren(@PathVariable Integer id) {
        long count = categoryService.countChildren(id);
        return ResponseEntity.ok(ApiResponse.success(Map.of("categoryId", id, "childrenCount", count)));
    }

    @GetMapping("/page")
    @Operation(summary = "Get paginated active categories with keyword filter")
    public ResponseEntity<ApiResponse<PageResponse<CategoryResponse>>> getCategoriesPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "sortOrder") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        PageResponse<CategoryResponse> result = categoryService.getCategoriesPaginated(page, size, keyword, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.success("Categories page retrieved successfully", result));
    }

    @GetMapping("/trash")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('CATEGORY_MANAGE')")
    @Operation(summary = "Get deleted (trashed) categories")
    public ResponseEntity<ApiResponse<PageResponse<CategoryResponse>>> getDeletedCategories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success("Deleted categories retrieved", categoryService.getDeletedCategories(page, size)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get category by ID (Cached)")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getCategoryById(id)));
    }

    @GetMapping("/slug/{slug}")
    @Operation(summary = "Get category by slug (Cached)")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getCategoryBySlug(slug)));
    }

    // ── Write ─────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('CATEGORY_MANAGE')")
    @Operation(summary = "Create a new category")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(@Valid @RequestBody CategoryRequest request) {
        CategoryResponse created = categoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Category created successfully", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('CATEGORY_MANAGE')")
    @Operation(summary = "Update an existing category")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable Integer id,
            @Valid @RequestBody CategoryRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Category updated successfully", categoryService.updateCategory(id, request)));
    }

    // ── Soft-delete / Restore / Bulk ──────────────────────────────

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('CATEGORY_MANAGE')")
    @Operation(summary = "Soft-delete a category and all descendants (moves to trash)")
    public ResponseEntity<ApiResponse<Void>> softDeleteCategory(@PathVariable Integer id) {
        categoryService.softDeleteCategory(id);
        return ResponseEntity.ok(ApiResponse.success("Category and descendants moved to trash", null));
    }

    @PatchMapping("/{id}/restore")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('CATEGORY_MANAGE')")
    @Operation(summary = "Restore a soft-deleted category and all descendants")
    public ResponseEntity<ApiResponse<Void>> restoreCategory(@PathVariable Integer id) {
        categoryService.restoreCategory(id);
        return ResponseEntity.ok(ApiResponse.success("Category and descendants restored successfully", null));
    }

    @PatchMapping("/bulk")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('CATEGORY_MANAGE')")
    @Operation(summary = "Bulk action on categories (delete/restore)")
    public ResponseEntity<ApiResponse<BulkActionResult>> bulkAction(@Valid @RequestBody BulkActionRequest request) {
        BulkActionResult result = categoryService.bulkAction(request);
        return ResponseEntity.ok(ApiResponse.success("Bulk action completed", result));
    }
}
