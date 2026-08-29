package com.store.controller;

import com.store.dto.response.ApiResponse;
import com.store.dto.response.banner.BannerResponse;
import com.store.entity.banner.BannerPosition;
import com.store.service.BannerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/banners")
@RequiredArgsConstructor
@Tag(name = "Banners", description = "Public Promotional & Hero Banner APIs")
public class BannerController {

    private final BannerService bannerService;

    @GetMapping("/public")
    @Operation(summary = "Get active banners for specific position within date validity range (Cached 1h)")
    public ResponseEntity<ApiResponse<List<BannerResponse>>> getPublicBanners(
            @RequestParam(required = false) String position
    ) {
        BannerPosition bannerPosition = null;
        if (position != null && !position.isBlank()) {
            try {
                bannerPosition = BannerPosition.fromValue(position);
            } catch (IllegalArgumentException e) {
                // Ignore invalid position and return empty list or all
            }
        }
        List<BannerResponse> banners = bannerService.getPublicBanners(bannerPosition);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách banner thành công", banners));
    }
}
