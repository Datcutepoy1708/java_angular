package com.store.controller;

import com.store.dto.request.banner.CreateBannerRequest;
import com.store.dto.request.banner.UpdateBannerRequest;
import com.store.dto.response.ApiResponse;
import com.store.dto.response.banner.BannerResponse;
import com.store.entity.banner.BannerPosition;
import com.store.service.BannerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

@RestController
@RequestMapping("/api/v1/admin/banners")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
@Tag(name = "Admin Banners", description = "Admin Banner Management APIs")
public class AdminBannerController {

    private final BannerService bannerService;

    @GetMapping
    @Operation(summary = "Get all banners for admin with optional position filter")
    public ResponseEntity<ApiResponse<List<BannerResponse>>> getAdminBanners(
            @RequestParam(required = false) BannerPosition position
    ) {
        List<BannerResponse> banners = bannerService.getAdminBanners(position);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách banner quản trị thành công", banners));
    }

    @GetMapping("/{bannerId}")
    @Operation(summary = "Get banner details by ID")
    public ResponseEntity<ApiResponse<BannerResponse>> getBannerById(@PathVariable Long bannerId) {
        BannerResponse banner = bannerService.getBannerById(bannerId);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin banner thành công", banner));
    }

    @PostMapping
    @Operation(summary = "Create new promotional or hero banner")
    public ResponseEntity<ApiResponse<BannerResponse>> createBanner(@Valid @RequestBody CreateBannerRequest request) {
        BannerResponse banner = bannerService.createBanner(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo banner thành công", banner));
    }

    @PutMapping("/{bannerId}")
    @Operation(summary = "Update existing banner")
    public ResponseEntity<ApiResponse<BannerResponse>> updateBanner(
            @PathVariable Long bannerId,
            @Valid @RequestBody UpdateBannerRequest request
    ) {
        BannerResponse banner = bannerService.updateBanner(bannerId, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật banner thành công", banner));
    }

    @DeleteMapping("/{bannerId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete banner by ID")
    public ResponseEntity<ApiResponse<Void>> deleteBanner(@PathVariable Long bannerId) {
        bannerService.deleteBanner(bannerId);
        return ResponseEntity.ok(ApiResponse.success("Xóa banner thành công", null));
    }
}
