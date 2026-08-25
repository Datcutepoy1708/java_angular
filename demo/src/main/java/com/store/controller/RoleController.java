package com.store.controller;

import com.store.dto.response.ApiResponse;
import com.store.dto.role.RoleCreateRequest;
import com.store.dto.role.RoleDetailResponse;
import com.store.dto.role.RolePermissionsUpdateRequest;
import com.store.dto.role.RoleUpdateRequest;
import com.store.service.RoleService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/roles")
@RequiredArgsConstructor
@Tag(name = "Admin Roles", description = "APIs for managing custom roles and permission matrix")
@PreAuthorize("hasRole('ADMIN')")
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    @Operation(summary = "Get all roles with user count and permission sets")
    public ResponseEntity<ApiResponse<List<RoleDetailResponse>>> getAllRoles() {
        List<RoleDetailResponse> response = roleService.getAllRoles();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách chức vụ thành công", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get role details by ID")
    public ResponseEntity<ApiResponse<RoleDetailResponse>> getRoleById(@PathVariable("id") Integer roleId) {
        RoleDetailResponse response = roleService.getRoleById(roleId);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin chức vụ thành công", response));
    }

    @PostMapping
    @Operation(summary = "Create a new custom role")
    public ResponseEntity<ApiResponse<RoleDetailResponse>> createRole(@Valid @RequestBody RoleCreateRequest request) {
        RoleDetailResponse response = roleService.createRole(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo chức vụ mới thành công", response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update role name / description")
    public ResponseEntity<ApiResponse<RoleDetailResponse>> updateRole(
            @PathVariable("id") Integer roleId,
            @Valid @RequestBody RoleUpdateRequest request
    ) {
        RoleDetailResponse response = roleService.updateRole(roleId, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thông tin chức vụ thành công", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete custom role (must not be system role and have 0 users)")
    public ResponseEntity<ApiResponse<Void>> deleteRole(@PathVariable("id") Integer roleId) {
        roleService.deleteRole(roleId);
        return ResponseEntity.ok(ApiResponse.success("Xóa chức vụ thành công", null));
    }

    @PutMapping("/{id}/permissions")
    @Operation(summary = "Update assigned permissions for a role (matrix save)")
    public ResponseEntity<ApiResponse<RoleDetailResponse>> updateRolePermissions(
            @PathVariable("id") Integer roleId,
            @Valid @RequestBody RolePermissionsUpdateRequest request
    ) {
        RoleDetailResponse response = roleService.updateRolePermissions(roleId, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật quyền cho chức vụ thành công", response));
    }
}
