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
    @Operation(summary = "Get images of a product", description = "Retrieve all gallery images of a specific product")
    public ResponseEntity<ApiResponse<List<ProductImageResponse>>> getImagesByProductId(@PathVariable Long productId) {
        List<ProductImageResponse> images = productImageService.getImagesByProductId(productId);
        return ResponseEntity.ok(ApiResponse.success("Product images retrieved successfully", images));
    }

    @PostMapping("/api/v1/products/{productId}/images")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN') or hasAuthority('PRODUCT_UPDATE')")
    @Operation(summary = "Add image to product", description = "Add a new image to product or variant and evict product cache")
    public ResponseEntity<ApiResponse<ProductImageResponse>> addImage(
            @PathVariable Long productId,
            @Valid @RequestBody ProductImageRequest request
    ) {
        ProductImageResponse createdImage = productImageService.addImage(productId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product image added successfully", createdImage));
    }

    @GetMapping("/api/v1/variants/{variantId}/images")
    @Operation(summary = "Get images of a variant", description = "Retrieve all images associated with a specific product variant")
    public ResponseEntity<ApiResponse<List<ProductImageResponse>>> getImagesByVariantId(@PathVariable Long variantId) {
        List<ProductImageResponse> images = productImageService.getImagesByVariantId(variantId);
        return ResponseEntity.ok(ApiResponse.success("Variant images retrieved successfully", images));
    }

    @PatchMapping("/api/v1/images/{id}/main")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN') or hasAuthority('PRODUCT_UPDATE')")
    @Operation(summary = "Set as main image", description = "Set an image as the primary cover photo, demoting any existing main image")
    public ResponseEntity<ApiResponse<ProductImageResponse>> setMainImage(@PathVariable Long id) {
        ProductImageResponse updated = productImageService.setMainImage(id);
        return ResponseEntity.ok(ApiResponse.success("Image set as main successfully", updated));
    }

    @PutMapping("/api/v1/images/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN') or hasAuthority('PRODUCT_UPDATE')")
    @Operation(summary = "Update image details", description = "Update image URL, sort order, or alt text")
    public ResponseEntity<ApiResponse<ProductImageResponse>> updateImage(
            @PathVariable Long id,
            @Valid @RequestBody ProductImageRequest request
    ) {
        ProductImageResponse updated = productImageService.updateImage(id, request);
        return ResponseEntity.ok(ApiResponse.success("Image updated successfully", updated));
    }

    @DeleteMapping("/api/v1/images/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN') or hasAuthority('PRODUCT_DELETE')")
    @Operation(summary = "Delete an image", description = "Delete an image from the gallery and evict product cache")
    public ResponseEntity<ApiResponse<Void>> deleteImage(@PathVariable Long id) {
        productImageService.deleteImage(id);
        return ResponseEntity.ok(ApiResponse.success("Image deleted successfully", null));
    }

    @PutMapping("/api/v1/products/{productId}/images/reorder")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN') or hasAuthority('PRODUCT_UPDATE')")
    @Operation(summary = "Reorder product images", description = "Update sort order of images for drag & drop gallery sorting")
    public ResponseEntity<ApiResponse<Void>> reorderImages(
            @PathVariable Long productId,
            @RequestBody List<Long> imageIds
    ) {
        productImageService.updateSortOrders(productId, imageIds);
        return ResponseEntity.ok(ApiResponse.success("Images reordered successfully", null));
    }
}
