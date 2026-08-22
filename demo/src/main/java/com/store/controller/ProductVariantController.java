package com.store.controller;

import com.store.dto.request.ProductVariantRequest;
import com.store.dto.response.ApiResponse;
import com.store.dto.response.ProductVariantResponse;
import com.store.service.ProductVariantService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Product Variant", description = "Product Variant management APIs")
public class ProductVariantController {

    private final ProductVariantService productVariantService;

    @GetMapping("/api/v1/products/{productId}/variants")
    @Operation(summary = "Get variants of a product", description = "Retrieve all variants belonging to a specific product")
    public ResponseEntity<ApiResponse<List<ProductVariantResponse>>> getVariantsByProductId(@PathVariable Long productId) {
        List<ProductVariantResponse> variants = productVariantService.getVariantsByProductId(productId);
        return ResponseEntity.ok(ApiResponse.success("Product variants retrieved successfully", variants));
    }

    @PostMapping("/api/v1/products/{productId}/variants")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN') or hasAuthority('PRODUCT_CREATE')")
    @Operation(summary = "Create a variant for a product", description = "Add a new variant under a specific product and evict product cache")
    public ResponseEntity<ApiResponse<ProductVariantResponse>> createVariant(
            @PathVariable Long productId,
            @Valid @RequestBody ProductVariantRequest request
    ) {
        ProductVariantResponse createdVariant = productVariantService.createVariant(productId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product variant created successfully", createdVariant));
    }

    @GetMapping("/api/v1/variants/{id}")
    @Operation(summary = "Get variant by ID", description = "Retrieve a single variant by primary key ID")
    public ResponseEntity<ApiResponse<ProductVariantResponse>> getVariantById(@PathVariable Long id) {
        ProductVariantResponse variant = productVariantService.getVariantById(id);
        return ResponseEntity.ok(ApiResponse.success(variant));
    }

    @GetMapping("/api/v1/variants/sku/{sku}")
    @Operation(summary = "Get variant by SKU", description = "Retrieve a single variant by SKU code")
    public ResponseEntity<ApiResponse<ProductVariantResponse>> getVariantBySku(@PathVariable String sku) {
        ProductVariantResponse variant = productVariantService.getVariantBySku(sku);
        return ResponseEntity.ok(ApiResponse.success(variant));
    }

    @PutMapping("/api/v1/variants/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN') or hasAuthority('PRODUCT_UPDATE')")
    @Operation(summary = "Update a variant", description = "Update variant details and evict product cache")
    public ResponseEntity<ApiResponse<ProductVariantResponse>> updateVariant(
            @PathVariable Long id,
            @Valid @RequestBody ProductVariantRequest request
    ) {
        ProductVariantResponse updatedVariant = productVariantService.updateVariant(id, request);
        return ResponseEntity.ok(ApiResponse.success("Product variant updated successfully", updatedVariant));
    }

    @DeleteMapping("/api/v1/variants/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN') or hasAuthority('PRODUCT_DELETE')")
    @Operation(summary = "Delete a variant", description = "Delete a variant and evict product cache")
    public ResponseEntity<ApiResponse<Void>> deleteVariant(@PathVariable Long id) {
        productVariantService.deleteVariant(id);
        return ResponseEntity.ok(ApiResponse.success("Product variant deleted successfully", null));
    }
}
