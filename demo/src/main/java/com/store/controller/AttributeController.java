package com.store.controller;

import com.store.dto.request.attribute.AttributeRequest;
import com.store.dto.response.ApiResponse;
import com.store.dto.response.attribute.AttributeResponse;
import com.store.service.AttributeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/attributes")
@RequiredArgsConstructor
@Tag(name = "Product Attributes (EAV)", description = "APIs for category technical attribute specifications")
public class AttributeController {

    private final AttributeService attributeService;

    @GetMapping("/category/{categoryId}")
    @Operation(summary = "Get attributes by category", description = "Retrieve list of attributes defined for a specific category")
    public ResponseEntity<ApiResponse<List<AttributeResponse>>> getAttributesByCategory(@PathVariable Integer categoryId) {
        List<AttributeResponse> attributes = attributeService.getAttributesByCategory(categoryId);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách thuộc tính thành công", attributes));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get attribute by ID", description = "Retrieve attribute details by attribute ID")
    public ResponseEntity<ApiResponse<AttributeResponse>> getAttributeById(@PathVariable Integer id) {
        AttributeResponse response = attributeService.getAttributeById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin thuộc tính thành công", response));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('ATTRIBUTE_MANAGE')")
    @Operation(summary = "Create attribute", description = "Define a new technical attribute for a category")
    public ResponseEntity<ApiResponse<AttributeResponse>> createAttribute(@Valid @RequestBody AttributeRequest request) {
        AttributeResponse created = attributeService.createAttribute(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo thuộc tính thành công", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('ATTRIBUTE_MANAGE')")
    @Operation(summary = "Update attribute", description = "Update technical attribute definition")
    public ResponseEntity<ApiResponse<AttributeResponse>> updateAttribute(
            @PathVariable Integer id,
            @Valid @RequestBody AttributeRequest request
    ) {
        AttributeResponse updated = attributeService.updateAttribute(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thuộc tính thành công", updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('ATTRIBUTE_MANAGE')")
    @Operation(summary = "Delete attribute", description = "Delete a technical attribute definition")
    public ResponseEntity<ApiResponse<Void>> deleteAttribute(@PathVariable Integer id) {
        attributeService.deleteAttribute(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa thuộc tính thành công", null));
    }
}
