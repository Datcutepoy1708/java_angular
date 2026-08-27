package com.store.controller;

import com.store.dto.request.attribute.BatchSaveProductAttributesRequest;
import com.store.dto.response.ApiResponse;
import com.store.dto.response.attribute.ProductAttributeValueResponse;
import com.store.service.ProductAttributeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products/{productId}/attributes")
@RequiredArgsConstructor
@Tag(name = "Product Attribute Values", description = "APIs for product technical specifications values")
public class ProductAttributeController {

    private final ProductAttributeService productAttributeService;

    @GetMapping
    @Operation(summary = "Get product attributes", description = "Retrieve list of technical specifications assigned to a product")
    public ResponseEntity<ApiResponse<List<ProductAttributeValueResponse>>> getProductAttributes(@PathVariable Long productId) {
        List<ProductAttributeValueResponse> list = productAttributeService.getProductAttributes(productId);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông số kỹ thuật sản phẩm thành công", list));
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('PRODUCT_UPDATE')")
    @Operation(summary = "Batch save product attributes", description = "Set or update technical specifications for a product")
    public ResponseEntity<ApiResponse<List<ProductAttributeValueResponse>>> saveProductAttributes(
            @PathVariable Long productId,
            @Valid @RequestBody BatchSaveProductAttributesRequest request
    ) {
        List<ProductAttributeValueResponse> saved = productAttributeService.saveProductAttributes(productId, request);
        return ResponseEntity.ok(ApiResponse.success("Lưu thông số kỹ thuật sản phẩm thành công", saved));
    }

    @DeleteMapping("/{attributeId}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('PRODUCT_UPDATE')")
    @Operation(summary = "Delete single product attribute", description = "Remove a technical specification from a product")
    public ResponseEntity<ApiResponse<Void>> deleteProductAttribute(
            @PathVariable Long productId,
            @PathVariable Integer attributeId
    ) {
        productAttributeService.deleteProductAttribute(productId, attributeId);
        return ResponseEntity.ok(ApiResponse.success("Xóa thông số kỹ thuật thành công", null));
    }
}
