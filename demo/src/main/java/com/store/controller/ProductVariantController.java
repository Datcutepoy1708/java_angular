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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Product Variant", description = "Product Variant management APIs")
public class ProductVariantController {

    private final ProductVariantService productVariantService;

    @GetMapping("/api/v1/products/{productId}/variants")
    @Operation(summary = "Get active variants of a product")
    public ResponseEntity<ApiResponse<List<ProductVariantResponse>>> getVariantsByProductId(@PathVariable Long productId) {
        return ResponseEntity.ok(ApiResponse.success("Product variants retrieved successfully",
                productVariantService.getVariantsByProductId(productId)));
    }

    @GetMapping("/api/v1/products/{productId}/variants/deleted")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('PRODUCT_UPDATE')")
    @Operation(summary = "Get soft-deleted (hidden) variants for a product")
    public ResponseEntity<ApiResponse<List<ProductVariantResponse>>> getDeletedVariants(@PathVariable Long productId) {
        return ResponseEntity.ok(ApiResponse.success("Deleted variants retrieved",
                productVariantService.getDeletedVariantsByProductId(productId)));
    }

    @PostMapping("/api/v1/products/{productId}/variants")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('PRODUCT_CREATE')")
    @Operation(summary = "Create a variant for a product")
    public ResponseEntity<ApiResponse<ProductVariantResponse>> createVariant(
            @PathVariable Long productId,
            @Valid @RequestBody ProductVariantRequest request
    ) {
        ProductVariantResponse created = productVariantService.createVariant(productId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product variant created successfully", created));
    }

    @GetMapping("/api/v1/variants/{id}")
    @Operation(summary = "Get variant by ID")
    public ResponseEntity<ApiResponse<ProductVariantResponse>> getVariantById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(productVariantService.getVariantById(id)));
    }

    @GetMapping("/api/v1/variants/sku/{sku}")
    @Operation(summary = "Get variant by SKU")
    public ResponseEntity<ApiResponse<ProductVariantResponse>> getVariantBySku(@PathVariable String sku) {
        return ResponseEntity.ok(ApiResponse.success(productVariantService.getVariantBySku(sku)));
    }

    @PutMapping("/api/v1/variants/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('PRODUCT_UPDATE')")
    @Operation(summary = "Update a variant")
    public ResponseEntity<ApiResponse<ProductVariantResponse>> updateVariant(
            @PathVariable Long id,
            @Valid @RequestBody ProductVariantRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Product variant updated successfully",
                productVariantService.updateVariant(id, request)));
    }

    @DeleteMapping("/api/v1/variants/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('PRODUCT_DELETE')")
    @Operation(summary = "Soft-delete a variant (moves to hidden)")
    public ResponseEntity<ApiResponse<Void>> softDeleteVariant(@PathVariable Long id) {
        productVariantService.softDeleteVariant(id);
        return ResponseEntity.ok(ApiResponse.success("Product variant hidden (soft-deleted)", null));
    }

    @PatchMapping("/api/v1/variants/{id}/restore")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('PRODUCT_UPDATE')")
    @Operation(summary = "Restore a soft-deleted variant")
    public ResponseEntity<ApiResponse<Void>> restoreVariant(@PathVariable Long id) {
        productVariantService.restoreVariant(id);
        return ResponseEntity.ok(ApiResponse.success("Product variant restored successfully", null));
    }
}
