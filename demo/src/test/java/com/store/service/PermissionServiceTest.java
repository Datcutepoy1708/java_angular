package com.store.service;

import com.store.dto.role.PermissionGroupResponse;
import com.store.dto.role.PermissionResponse;
import com.store.entity.user.Permission;
import com.store.repository.PermissionRepository;
import com.store.service.impl.PermissionServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {

    @Mock
    private PermissionRepository permissionRepository;

    @InjectMocks
    private PermissionServiceImpl permissionService;

    @Test
    @DisplayName("getAllGroupedPermissions should categorize permissions into functional module groups")
    void getAllGroupedPermissions_Success() {
        Permission p1 = Permission.builder().permissionId(1).permissionCode("PRODUCT_VIEW").description("Xem SP").build();
        Permission p2 = Permission.builder().permissionId(2).permissionCode("ORDER_VIEW").description("Xem đơn").build();
        Permission p3 = Permission.builder().permissionId(3).permissionCode("INVENTORY_MANAGE").description("Quản lý kho").build();
        Permission p4 = Permission.builder().permissionId(4).permissionCode("ROLE_MANAGE").description("Quản lý quyền").build();

        when(permissionRepository.findAllByOrderByPermissionCodeAsc()).thenReturn(List.of(p1, p2, p3, p4));

        List<PermissionGroupResponse> result = permissionService.getAllGroupedPermissions();

        assertThat(result).isNotEmpty();
        assertThat(result.stream().map(PermissionGroupResponse::getGroupCode))
                .contains("PRODUCT", "ORDER", "INVENTORY", "USER");
    }

    @Test
    @DisplayName("getAllPermissions should return flat list")
    void getAllPermissions_Success() {
        Permission p1 = Permission.builder().permissionId(1).permissionCode("PRODUCT_VIEW").description("Xem SP").build();
        when(permissionRepository.findAllByOrderByPermissionCodeAsc()).thenReturn(List.of(p1));

        List<PermissionResponse> result = permissionService.getAllPermissions();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPermissionCode()).isEqualTo("PRODUCT_VIEW");
    }
}
