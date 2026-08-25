package com.store.controller;

import com.store.dto.response.ApiResponse;
import com.store.dto.role.PermissionGroupResponse;
import com.store.service.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/permissions")
@RequiredArgsConstructor
@Tag(name = "Admin Permissions", description = "APIs for managing system permissions and permission groups")
@PreAuthorize("hasRole('ADMIN')")
public class PermissionController {

    private final PermissionService permissionService;

    @GetMapping
    @Operation(summary = "Get all grouped permissions for matrix management")
    public ResponseEntity<ApiResponse<List<PermissionGroupResponse>>> getAllGroupedPermissions() {
        List<PermissionGroupResponse> response = permissionService.getAllGroupedPermissions();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách nhóm quyền thành công", response));
    }
}
