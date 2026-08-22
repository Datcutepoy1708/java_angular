package com.store.controller;

import com.store.dto.request.CategoryRequest;
import com.store.dto.response.ApiResponse;
import com.store.dto.response.CategoryResponse;
import com.store.dto.response.PageResponse;
import com.store.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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

    @GetMapping
    @Operation(summary = "Get all categories", description = "Retrieve a flat list of all categories (Cached)")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllCategories() {
        List<CategoryResponse> categories = categoryService.getAllCategories();
        return ResponseEntity.ok(ApiResponse.success("Categories retrieved successfully", categories));
    }

    @GetMapping("/tree")
    @Operation(summary = "Get category tree", description = "Retrieve categories as a hierarchical tree from root down to subcategories (Cached)")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getCategoryTree() {
        List<CategoryResponse> tree = categoryService.getCategoryTree();
        return ResponseEntity.ok(ApiResponse.success("Category tree retrieved successfully", tree));
    }

    @GetMapping("/roots")
    @Operation(summary = "Get root categories", description = "Retrieve only top-level root categories without parent (Cached)")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getRootCategories() {
        List<CategoryResponse> roots = categoryService.getRootCategories();
        return ResponseEntity.ok(ApiResponse.success("Root categories retrieved successfully", roots));
    }

    @GetMapping("/{id}/children")
    @Operation(summary = "Get direct children", description = "Retrieve direct subcategories of a specified parent category (Cached)")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getChildren(@PathVariable Integer id) {
        List<CategoryResponse> children = categoryService.getChildrenByParentId(id);
        return ResponseEntity.ok(ApiResponse.success("Children categories retrieved successfully", children));
    }

    @GetMapping("/{id}/children/count")
    @Operation(summary = "Count direct children", description = "Get the number of subcategories for pre-delete warning in admin panel")
    public ResponseEntity<ApiResponse<Map<String, Object>>> countChildren(@PathVariable Integer id) {
        long count = categoryService.countChildren(id);
        return ResponseEntity.ok(ApiResponse.success(Map.of("categoryId", id, "childrenCount", count)));
    }

    @GetMapping("/page")
    @Operation(summary = "Get paginated categories", description = "Retrieve categories with pagination and sorting")
    public ResponseEntity<ApiResponse<PageResponse<CategoryResponse>>> getCategoriesPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "sortOrder") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        PageResponse<CategoryResponse> result = categoryService.getCategoriesPaginated(page, size, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.success("Categories page retrieved successfully", result));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get category by ID", description = "Retrieve a single category by primary key ID (Cached)")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryById(@PathVariable Integer id) {
        CategoryResponse category = categoryService.getCategoryById(id);
        return ResponseEntity.ok(ApiResponse.success(category));
    }

    @GetMapping("/slug/{slug}")
    @Operation(summary = "Get category by slug", description = "Retrieve a single category by URL slug (Cached)")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryBySlug(@PathVariable String slug) {
        CategoryResponse category = categoryService.getCategoryBySlug(slug);
        return ResponseEntity.ok(ApiResponse.success(category));
    }

    @PostMapping
    @Operation(summary = "Create a new category", description = "Create a new category and evict cache")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(@Valid @RequestBody CategoryRequest request) {
        CategoryResponse createdCategory = categoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Category created successfully", createdCategory));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing category", description = "Update category details, validate hierarchy, and evict cache")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable Integer id,
            @Valid @RequestBody CategoryRequest request
    ) {
        CategoryResponse updatedCategory = categoryService.updateCategory(id, request);
        return ResponseEntity.ok(ApiResponse.success("Category updated successfully", updatedCategory));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a category", description = "Delete category by ID (children will have parent_id set to null)")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable Integer id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.success("Category deleted successfully", null));
    }
}
