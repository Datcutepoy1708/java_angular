package com.store.controller;

import com.store.dto.request.BulkActionRequest;
import com.store.dto.request.ProductFilterRequest;
import com.store.dto.request.ProductRequest;
import com.store.dto.response.ApiResponse;
import com.store.dto.response.BulkActionResult;
import com.store.dto.response.PageResponse;
import com.store.dto.response.ProductResponse;
import com.store.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Product", description = "Product management APIs with dynamic search, filtering, and Redis caching")
public class ProductController {

    private final ProductService productService;

    // ── Read ──────────────────────────────────────────────────────

    @GetMapping
    @Operation(summary = "Get products with filters and pagination (active only)")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getProducts(@ModelAttribute ProductFilterRequest filter) {
        return ResponseEntity.ok(ApiResponse.success("Products retrieved successfully", productService.getProducts(filter)));
    }

    @GetMapping("/trash")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('PRODUCT_DELETE')")
    @Operation(summary = "Get deleted (trashed) products")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getDeletedProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success("Deleted products retrieved", productService.getDeletedProducts(page, size)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID (Cached)")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(productService.getProductById(id)));
    }

    @GetMapping("/slug/{slug}")
    @Operation(summary = "Get product by slug (Cached)")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.success(productService.getProductBySlug(slug)));
    }

    // ── Write ─────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('PRODUCT_CREATE')")
    @Operation(summary = "Create a new product")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(@Valid @RequestBody ProductRequest request) {
        ProductResponse created = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product created successfully", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('PRODUCT_UPDATE')")
    @Operation(summary = "Update an existing product")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Product updated successfully", productService.updateProduct(id, request)));
    }

    // ── Soft-delete / Restore / Bulk ──────────────────────────────

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('PRODUCT_DELETE')")
    @Operation(summary = "Soft-delete a product (moves to trash)")
    public ResponseEntity<ApiResponse<Void>> softDeleteProduct(@PathVariable Long id) {
        productService.softDeleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Product moved to trash", null));
    }

    @PatchMapping("/{id}/restore")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('PRODUCT_DELETE')")
    @Operation(summary = "Restore a soft-deleted product")
    public ResponseEntity<ApiResponse<Void>> restoreProduct(@PathVariable Long id) {
        productService.restoreProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Product restored successfully", null));
    }

    @PatchMapping("/bulk")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('PRODUCT_DELETE')")
    @Operation(summary = "Bulk action on products (delete/restore)")
    public ResponseEntity<ApiResponse<BulkActionResult>> bulkAction(@Valid @RequestBody BulkActionRequest request) {
        BulkActionResult result = productService.bulkAction(request);
        return ResponseEntity.ok(ApiResponse.success("Bulk action completed", result));
    }

    @PatchMapping("/{id}/view")
    @Operation(summary = "Increment product view count")
    public ResponseEntity<ApiResponse<Void>> incrementViewCount(@PathVariable Long id) {
        productService.incrementViewCount(id);
        return ResponseEntity.ok(ApiResponse.success("View count incremented", null));
    }
}
