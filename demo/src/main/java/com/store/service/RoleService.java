package com.store.service;

import com.store.dto.role.RoleCreateRequest;
import com.store.dto.role.RoleDetailResponse;
import com.store.dto.role.RolePermissionsUpdateRequest;
import com.store.dto.role.RoleUpdateRequest;

import java.util.List;

public interface RoleService {

    List<RoleDetailResponse> getAllRoles();

    RoleDetailResponse getRoleById(Integer roleId);

    RoleDetailResponse createRole(RoleCreateRequest request);

    RoleDetailResponse updateRole(Integer roleId, RoleUpdateRequest request);

    void deleteRole(Integer roleId);

    RoleDetailResponse updateRolePermissions(Integer roleId, RolePermissionsUpdateRequest request);
}
