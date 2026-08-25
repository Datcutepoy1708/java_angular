package com.store.service;

import com.store.dto.role.RoleCreateRequest;
import com.store.dto.role.RoleDetailResponse;
import com.store.dto.role.RolePermissionsUpdateRequest;
import com.store.dto.role.RoleUpdateRequest;
import com.store.entity.user.Permission;
import com.store.entity.user.Role;
import com.store.exception.BadRequestException;
import com.store.exception.DuplicateResourceException;
import com.store.exception.ResourceNotFoundException;
import com.store.repository.PermissionRepository;
import com.store.repository.RoleRepository;
import com.store.repository.UserRepository;
import com.store.service.impl.RoleServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RoleServiceImpl roleService;

    private Role adminRole;
    private Role staffRole;
    private Role customerRole;
    private Role customRole;
    private Permission permProductView;
    private Permission permProductCreate;
    private Permission permRoleManage;
    private Permission permStaffManage;
    private Permission permSettingManage;

    @BeforeEach
    void setUp() {
        permProductView = Permission.builder().permissionId(1).permissionCode("PRODUCT_VIEW").description("Xem sản phẩm").build();
        permProductCreate = Permission.builder().permissionId(2).permissionCode("PRODUCT_CREATE").description("Tạo sản phẩm").build();
        permRoleManage = Permission.builder().permissionId(3).permissionCode("ROLE_MANAGE").description("Quản lý chức vụ").build();
        permStaffManage = Permission.builder().permissionId(4).permissionCode("STAFF_MANAGE").description("Quản lý nhân viên").build();
        permSettingManage = Permission.builder().permissionId(5).permissionCode("SETTING_MANAGE").description("Cài đặt hệ thống").build();

        adminRole = Role.builder()
                .roleId(1)
                .roleName("ROLE_ADMIN")
                .description("Quản trị hệ thống")
                .permissions(new HashSet<>(Set.of(permProductView, permProductCreate, permRoleManage, permStaffManage, permSettingManage)))
                .createdAt(LocalDateTime.now())
                .build();

        staffRole = Role.builder()
                .roleId(2)
                .roleName("ROLE_STAFF")
                .description("Nhân viên bán hàng")
                .permissions(new HashSet<>(Set.of(permProductView, permProductCreate)))
                .createdAt(LocalDateTime.now())
                .build();

        customerRole = Role.builder()
                .roleId(3)
                .roleName("ROLE_CUSTOMER")
                .description("Khách hàng")
                .permissions(new HashSet<>())
                .createdAt(LocalDateTime.now())
                .build();

        customRole = Role.builder()
                .roleId(10)
                .roleName("ROLE_WAREHOUSE_MANAGER")
                .description("Trưởng kho")
                .permissions(new HashSet<>(Set.of(permProductView)))
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("Read Operations")
    class ReadTests {

        @Test
        @DisplayName("getAllRoles should return all roles with user count and permission codes")
        void getAllRoles_Success() {
            when(roleRepository.findAllByOrderByRoleIdAsc()).thenReturn(List.of(adminRole, staffRole, customerRole, customRole));
            when(userRepository.countByRoles_RoleId(1)).thenReturn(2L);
            when(userRepository.countByRoles_RoleId(2)).thenReturn(5L);
            when(userRepository.countByRoles_RoleId(3)).thenReturn(100L);
            when(userRepository.countByRoles_RoleId(10)).thenReturn(1L);

            List<RoleDetailResponse> result = roleService.getAllRoles();

            assertThat(result).hasSize(4);
            assertThat(result.get(0).getRoleName()).isEqualTo("ROLE_ADMIN");
            assertThat(result.get(0).isSystemRole()).isTrue();
            assertThat(result.get(0).getUserCount()).isEqualTo(2L);
            assertThat(result.get(0).getPermissionCodes()).contains("ROLE_MANAGE", "PRODUCT_VIEW");

            assertThat(result.get(3).getRoleName()).isEqualTo("ROLE_WAREHOUSE_MANAGER");
            assertThat(result.get(3).isSystemRole()).isFalse();
            assertThat(result.get(3).getUserCount()).isEqualTo(1L);
        }

        @Test
        @DisplayName("getRoleById should return role detail when found")
        void getRoleById_Success() {
            when(roleRepository.findById(10)).thenReturn(Optional.of(customRole));
            when(userRepository.countByRoles_RoleId(10)).thenReturn(3L);

            RoleDetailResponse result = roleService.getRoleById(10);

            assertThat(result.getRoleId()).isEqualTo(10);
            assertThat(result.getRoleName()).isEqualTo("ROLE_WAREHOUSE_MANAGER");
            assertThat(result.getUserCount()).isEqualTo(3L);
        }

        @Test
        @DisplayName("getRoleById should throw ResourceNotFoundException when not found")
        void getRoleById_NotFound() {
            when(roleRepository.findById(999)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> roleService.getRoleById(999))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Create Role Operations")
    class CreateTests {

        @Test
        @DisplayName("createRole should normalize role code with ROLE_ prefix and save permissions")
        void createRole_Success() {
            RoleCreateRequest request = RoleCreateRequest.builder()
                    .roleName("accountant")
                    .description("Kế toán viên")
                    .permissionCodes(Set.of("PRODUCT_VIEW"))
                    .build();

            when(roleRepository.existsByRoleName("ROLE_ACCOUNTANT")).thenReturn(false);
            when(permissionRepository.findByPermissionCode("PRODUCT_VIEW")).thenReturn(Optional.of(permProductView));
            when(roleRepository.save(any(Role.class))).thenAnswer(i -> {
                Role r = i.getArgument(0);
                r.setRoleId(15);
                return r;
            });
            when(userRepository.countByRoles_RoleId(15)).thenReturn(0L);

            RoleDetailResponse result = roleService.createRole(request);

            assertThat(result.getRoleName()).isEqualTo("ROLE_ACCOUNTANT");
            assertThat(result.getDescription()).isEqualTo("Kế toán viên");
            assertThat(result.getPermissionCodes()).contains("PRODUCT_VIEW");
            verify(roleRepository).save(any(Role.class));
        }

        @Test
        @DisplayName("createRole should throw DuplicateResourceException when role code already exists")
        void createRole_DuplicateRoleName_ThrowsException() {
            RoleCreateRequest request = RoleCreateRequest.builder()
                    .roleName("ROLE_ADMIN")
                    .description("Duplicate")
                    .build();

            when(roleRepository.existsByRoleName("ROLE_ADMIN")).thenReturn(true);

            assertThatThrownBy(() -> roleService.createRole(request))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("ROLE_ADMIN");

            verify(roleRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Update Role Operations")
    class UpdateTests {

        @Test
        @DisplayName("updateRole should throw BadRequestException when renaming system role")
        void updateRole_SystemRole_ThrowsException() {
            RoleUpdateRequest request = RoleUpdateRequest.builder()
                    .roleName("ROLE_SUPER_ADMIN")
                    .build();

            when(roleRepository.findById(1)).thenReturn(Optional.of(adminRole));

            assertThatThrownBy(() -> roleService.updateRole(1, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Không thể thay đổi mã của chức vụ hệ thống mặc định");

            verify(roleRepository, never()).save(any());
        }

        @Test
        @DisplayName("updateRole should update custom role name and description")
        void updateRole_CustomRole_Success() {
            RoleUpdateRequest request = RoleUpdateRequest.builder()
                    .roleName("ROLE_HEAD_OF_WAREHOUSE")
                    .description("Trưởng phòng kho vận")
                    .build();

            when(roleRepository.findById(10)).thenReturn(Optional.of(customRole));
            when(roleRepository.existsByRoleNameAndRoleIdNot("ROLE_HEAD_OF_WAREHOUSE", 10)).thenReturn(false);
            when(roleRepository.save(any(Role.class))).thenAnswer(i -> i.getArgument(0));
            when(userRepository.countByRoles_RoleId(10)).thenReturn(1L);

            RoleDetailResponse result = roleService.updateRole(10, request);

            assertThat(result.getRoleName()).isEqualTo("ROLE_HEAD_OF_WAREHOUSE");
            assertThat(result.getDescription()).isEqualTo("Trưởng phòng kho vận");
            verify(roleRepository).save(any(Role.class));
        }
    }

    @Nested
    @DisplayName("Delete Role Operations & In-Use Guard")
    class DeleteTests {

        @Test
        @DisplayName("deleteRole should throw BadRequestException when deleting system role")
        void deleteRole_SystemRole_ThrowsException() {
            when(roleRepository.findById(1)).thenReturn(Optional.of(adminRole));

            assertThatThrownBy(() -> roleService.deleteRole(1))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Không thể xóa vai trò hệ thống mặc định");

            verify(roleRepository, never()).delete(any());
        }

        @Test
        @DisplayName("deleteRole should throw BadRequestException when role is currently assigned to users")
        void deleteRole_RoleInUse_ThrowsException() {
            when(roleRepository.findById(10)).thenReturn(Optional.of(customRole));
            when(userRepository.countByRoles_RoleId(10)).thenReturn(3L);

            assertThatThrownBy(() -> roleService.deleteRole(10))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("3 nhân sự sử dụng");

            verify(roleRepository, never()).delete(any());
        }

        @Test
        @DisplayName("deleteRole should delete custom role with 0 users successfully")
        void deleteRole_UnusedCustomRole_Success() {
            when(roleRepository.findById(10)).thenReturn(Optional.of(customRole));
            when(userRepository.countByRoles_RoleId(10)).thenReturn(0L);

            roleService.deleteRole(10);

            verify(roleRepository).delete(customRole);
        }
    }

    @Nested
    @DisplayName("Permission Matrix & Admin Depletion Guard")
    class PermissionMatrixTests {

        @Test
        @DisplayName("updateRolePermissions should throw BadRequestException when stripping all permissions from ROLE_ADMIN")
        void updateRolePermissions_AdminDepletion_Empty_ThrowsException() {
            RolePermissionsUpdateRequest request = RolePermissionsUpdateRequest.builder()
                    .permissionCodes(Set.of())
                    .build();

            when(roleRepository.findById(1)).thenReturn(Optional.of(adminRole));

            assertThatThrownBy(() -> roleService.updateRolePermissions(1, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Không thể gỡ bỏ toàn bộ quyền của vai trò Quản trị viên tối cao");

            verify(roleRepository, never()).save(any());
        }

        @Test
        @DisplayName("updateRolePermissions should throw BadRequestException when removing critical permissions from ROLE_ADMIN")
        void updateRolePermissions_AdminDepletion_Critical_ThrowsException() {
            // Missing ROLE_MANAGE
            RolePermissionsUpdateRequest request = RolePermissionsUpdateRequest.builder()
                    .permissionCodes(Set.of("PRODUCT_VIEW", "STAFF_MANAGE", "SETTING_MANAGE"))
                    .build();

            when(roleRepository.findById(1)).thenReturn(Optional.of(adminRole));

            assertThatThrownBy(() -> roleService.updateRolePermissions(1, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("ROLE_MANAGE");

            verify(roleRepository, never()).save(any());
        }

        @Test
        @DisplayName("updateRolePermissions should update permissions for custom role")
        void updateRolePermissions_CustomRole_Success() {
            RolePermissionsUpdateRequest request = RolePermissionsUpdateRequest.builder()
                    .permissionCodes(Set.of("PRODUCT_VIEW", "PRODUCT_CREATE"))
                    .build();

            when(roleRepository.findById(10)).thenReturn(Optional.of(customRole));
            when(permissionRepository.findByPermissionCode("PRODUCT_VIEW")).thenReturn(Optional.of(permProductView));
            when(permissionRepository.findByPermissionCode("PRODUCT_CREATE")).thenReturn(Optional.of(permProductCreate));
            when(roleRepository.save(any(Role.class))).thenAnswer(i -> i.getArgument(0));
            when(userRepository.countByRoles_RoleId(10)).thenReturn(1L);

            RoleDetailResponse result = roleService.updateRolePermissions(10, request);

            assertThat(result.getPermissionCodes()).containsExactlyInAnyOrder("PRODUCT_VIEW", "PRODUCT_CREATE");
            verify(roleRepository).save(any(Role.class));
        }
    }
}
