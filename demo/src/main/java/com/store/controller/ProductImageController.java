package com.store.controller;

import com.store.dto.request.ProductImageRequest;
import com.store.dto.response.ApiResponse;
import com.store.dto.response.ProductImageResponse;
import com.store.service.ProductImageService;
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
@Tag(name = "Product Image", description = "Product and Variant image gallery management APIs")
public class ProductImageController {

    private final ProductImageService productImageService;

    @GetMapping("/api/v1/products/{productId}/images")
    @Operation(summary = "Get active images of a product")
    public ResponseEntity<ApiResponse<List<ProductImageResponse>>> getImagesByProductId(@PathVariable Long productId) {
        return ResponseEntity.ok(ApiResponse.success("Product images retrieved successfully",
                productImageService.getImagesByProductId(productId)));
    }

    @GetMapping("/api/v1/products/{productId}/images/deleted")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('PRODUCT_UPDATE')")
    @Operation(summary = "Get soft-deleted (hidden) images for a product")
    public ResponseEntity<ApiResponse<List<ProductImageResponse>>> getDeletedImages(@PathVariable Long productId) {
        return ResponseEntity.ok(ApiResponse.success("Deleted images retrieved",
                productImageService.getDeletedImagesByProductId(productId)));
    }

    @PostMapping("/api/v1/products/{productId}/images")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('PRODUCT_UPDATE')")
    @Operation(summary = "Add image to product")
    public ResponseEntity<ApiResponse<ProductImageResponse>> addImage(
            @PathVariable Long productId,
            @Valid @RequestBody ProductImageRequest request
    ) {
        ProductImageResponse created = productImageService.addImage(productId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product image added successfully", created));
    }

    @GetMapping("/api/v1/variants/{variantId}/images")
    @Operation(summary = "Get images of a variant")
    public ResponseEntity<ApiResponse<List<ProductImageResponse>>> getImagesByVariantId(@PathVariable Long variantId) {
        return ResponseEntity.ok(ApiResponse.success("Variant images retrieved successfully",
                productImageService.getImagesByVariantId(variantId)));
    }

    @PatchMapping("/api/v1/images/{id}/main")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('PRODUCT_UPDATE')")
    @Operation(summary = "Set as main image")
    public ResponseEntity<ApiResponse<ProductImageResponse>> setMainImage(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Image set as main successfully",
                productImageService.setMainImage(id)));
    }

    @PutMapping("/api/v1/images/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('PRODUCT_UPDATE')")
    @Operation(summary = "Update image details")
    public ResponseEntity<ApiResponse<ProductImageResponse>> updateImage(
            @PathVariable Long id,
            @Valid @RequestBody ProductImageRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Image updated successfully",
                productImageService.updateImage(id, request)));
    }

    @DeleteMapping("/api/v1/images/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('PRODUCT_DELETE')")
    @Operation(summary = "Soft-delete an image (moves to hidden)")
    public ResponseEntity<ApiResponse<Void>> softDeleteImage(@PathVariable Long id) {
        productImageService.softDeleteImage(id);
        return ResponseEntity.ok(ApiResponse.success("Image hidden (soft-deleted)", null));
    }

    @PatchMapping("/api/v1/images/{id}/restore")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('PRODUCT_UPDATE')")
    @Operation(summary = "Restore a soft-deleted image")
    public ResponseEntity<ApiResponse<Void>> restoreImage(@PathVariable Long id) {
        productImageService.restoreImage(id);
        return ResponseEntity.ok(ApiResponse.success("Image restored successfully", null));
    }

    @PutMapping("/api/v1/products/{productId}/images/reorder")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('PRODUCT_UPDATE')")
    @Operation(summary = "Reorder product images (drag & drop)")
    public ResponseEntity<ApiResponse<Void>> reorderImages(
            @PathVariable Long productId,
            @RequestBody List<Long> imageIds
    ) {
        productImageService.updateSortOrders(productId, imageIds);
        return ResponseEntity.ok(ApiResponse.success("Images reordered successfully", null));
    }
}
