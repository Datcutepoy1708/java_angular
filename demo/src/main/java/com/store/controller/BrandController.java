package com.store.controller;

import com.store.dto.request.BrandRequest;
import com.store.dto.request.BulkActionRequest;
import com.store.dto.response.ApiResponse;
import com.store.dto.response.BrandResponse;
import com.store.dto.response.BulkActionResult;
import com.store.dto.response.PageResponse;
import com.store.service.BrandService;
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

@RestController
@RequestMapping("/api/v1/brands")
@RequiredArgsConstructor
@Tag(name = "Brand", description = "Brand management APIs")
public class BrandController {

    private final BrandService brandService;

    // ── Public / Read ──────────────────────────────────────────────

    @GetMapping
    @Operation(summary = "Get all active brands (Cached)")
    public ResponseEntity<ApiResponse<List<BrandResponse>>> getAllBrands() {
        return ResponseEntity.ok(ApiResponse.success("Brands retrieved successfully", brandService.getAllBrands()));
    }

    @GetMapping("/page")
    @Operation(summary = "Get paginated brands with filter")
    public ResponseEntity<ApiResponse<PageResponse<BrandResponse>>> getBrandsPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        PageResponse<BrandResponse> result = brandService.getBrandsPaginated(page, size, keyword, status, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.success("Brands page retrieved successfully", result));
    }

    @GetMapping("/trash")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('BRAND_MANAGE')")
    @Operation(summary = "Get deleted (trashed) brands")
    public ResponseEntity<ApiResponse<PageResponse<BrandResponse>>> getDeletedBrands(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success("Deleted brands retrieved", brandService.getDeletedBrands(page, size)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get brand by ID (Cached)")
    public ResponseEntity<ApiResponse<BrandResponse>> getBrandById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success(brandService.getBrandById(id)));
    }

    @GetMapping("/slug/{slug}")
    @Operation(summary = "Get brand by slug (Cached)")
    public ResponseEntity<ApiResponse<BrandResponse>> getBrandBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.success(brandService.getBrandBySlug(slug)));
    }

    // ── Write ─────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('BRAND_MANAGE')")
    @Operation(summary = "Create a new brand")
    public ResponseEntity<ApiResponse<BrandResponse>> createBrand(@Valid @RequestBody BrandRequest request) {
        BrandResponse created = brandService.createBrand(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Brand created successfully", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('BRAND_MANAGE')")
    @Operation(summary = "Update a brand")
    public ResponseEntity<ApiResponse<BrandResponse>> updateBrand(
            @PathVariable Integer id,
            @Valid @RequestBody BrandRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Brand updated successfully", brandService.updateBrand(id, request)));
    }

    // ── Soft-delete / Restore / Bulk ──────────────────────────────

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('BRAND_MANAGE')")
    @Operation(summary = "Soft-delete a brand (moves to trash)")
    public ResponseEntity<ApiResponse<Void>> softDeleteBrand(@PathVariable Integer id) {
        brandService.softDeleteBrand(id);
        return ResponseEntity.ok(ApiResponse.success("Brand moved to trash", null));
    }

    @PatchMapping("/{id}/restore")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('BRAND_MANAGE')")
    @Operation(summary = "Restore a soft-deleted brand")
    public ResponseEntity<ApiResponse<Void>> restoreBrand(@PathVariable Integer id) {
        brandService.restoreBrand(id);
        return ResponseEntity.ok(ApiResponse.success("Brand restored successfully", null));
    }

    @PatchMapping("/bulk")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('BRAND_MANAGE')")
    @Operation(summary = "Bulk action on brands (delete/restore/activate/deactivate)")
    public ResponseEntity<ApiResponse<BulkActionResult>> bulkAction(@Valid @RequestBody BulkActionRequest request) {
        BulkActionResult result = brandService.bulkAction(request);
        return ResponseEntity.ok(ApiResponse.success("Bulk action completed", result));
    }
}
