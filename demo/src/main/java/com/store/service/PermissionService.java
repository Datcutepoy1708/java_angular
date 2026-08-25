package com.store.service;

import com.store.dto.role.PermissionGroupResponse;
import com.store.dto.role.PermissionResponse;

import java.util.List;

public interface PermissionService {

    List<PermissionGroupResponse> getAllGroupedPermissions();

    List<PermissionResponse> getAllPermissions();
}
