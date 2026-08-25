package com.store.controller;

import com.store.dto.response.ApiResponse;
import com.store.dto.setting.SettingResponse;
import com.store.dto.setting.UpdateSettingsBatchRequest;
import com.store.entity.setting.SettingGroup;
import com.store.security.CustomUserDetails;
import com.store.service.SettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
@Tag(name = "Settings Management", description = "System and Storefront configuration APIs")
public class SettingController {

    private final SettingService settingService;

    @GetMapping("/public")
    @Operation(summary = "Get public system settings for storefront header/footer/SEO", description = "Unauthenticated public endpoint returning public configuration map")
    public ResponseEntity<ApiResponse<Map<String, String>>> getPublicSettings() {
        Map<String, String> settings = settingService.getPublicSettings();
        return ResponseEntity.ok(ApiResponse.success("Lấy cấu hình công khai thành công", settings));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get all system settings", description = "Restricted to administrators")
    public ResponseEntity<ApiResponse<List<SettingResponse>>> getAllSettings() {
        List<SettingResponse> settings = settingService.getAllSettings();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách cấu hình hệ thống thành công", settings));
    }

    @GetMapping("/grouped")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get settings grouped by category", description = "Restricted to administrators")
    public ResponseEntity<ApiResponse<Map<SettingGroup, List<SettingResponse>>>> getGroupedSettings() {
        Map<SettingGroup, List<SettingResponse>> grouped = settingService.getGroupedSettings();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách cấu hình phân nhóm thành công", grouped));
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update batch of system settings", description = "Restricted to administrators")
    public ResponseEntity<ApiResponse<Void>> updateSettings(
            @Valid @RequestBody UpdateSettingsBatchRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long currentUserId = userDetails != null ? userDetails.getUserId() : 1L;
        settingService.updateSettings(request, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật cấu hình hệ thống thành công", null));
    }

    @PostMapping("/reset-defaults")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Reset all settings to factory defaults", description = "Restricted to administrators")
    public ResponseEntity<ApiResponse<Void>> resetDefaults(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long currentUserId = userDetails != null ? userDetails.getUserId() : 1L;
        settingService.resetToDefaults(currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Đã khôi phục toàn bộ cấu hình về mặc định", null));
    }
}
