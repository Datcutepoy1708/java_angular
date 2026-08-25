package com.store.service;

import com.store.dto.role.RoleCreateRequest;
import com.store.dto.role.RoleDetailResponse;
import com.store.dto.role.RolePermissionsUpdateRequest;
import com.store.dto.role.RoleUpdateRequest;
import com.store.entity.user.AuthProvider;
import com.store.entity.user.Role;
import com.store.entity.user.User;
import com.store.entity.user.UserStatus;
import com.store.exception.BadRequestException;
import com.store.repository.RoleRepository;
import com.store.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = "jwt.secret=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970")
@Transactional
class RoleActiveGuardIntegrationTest {

    @Autowired
    private RoleService roleService;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Complete Role Lifecycle: Create -> Update Permissions -> Assign to Staff -> Block Delete -> Unassign -> Delete Success")
    void roleLifecycle_and_inUseGuard_Success() {
        long ts = System.currentTimeMillis();
        String roleCode = "ROLE_TEST_MANAGER_" + ts;

        // 1. Create custom role
        RoleCreateRequest createReq = RoleCreateRequest.builder()
                .roleName(roleCode)
                .description("Test Manager Description")
                .permissionCodes(Set.of("PRODUCT_VIEW", "ORDER_VIEW"))
                .build();

        RoleDetailResponse createdRole = roleService.createRole(createReq);
        assertThat(createdRole.getRoleId()).isNotNull();
        assertThat(createdRole.getRoleName()).isEqualTo(roleCode);
        assertThat(createdRole.isSystemRole()).isFalse();
        assertThat(createdRole.getPermissionCodes()).contains("PRODUCT_VIEW", "ORDER_VIEW");

        // 2. Update permissions via matrix endpoint
        RolePermissionsUpdateRequest permReq = RolePermissionsUpdateRequest.builder()
                .permissionCodes(Set.of("PRODUCT_VIEW", "ORDER_VIEW", "INVENTORY_VIEW"))
                .build();

        RoleDetailResponse updatedPerms = roleService.updateRolePermissions(createdRole.getRoleId(), permReq);
        assertThat(updatedPerms.getPermissionCodes()).contains("INVENTORY_VIEW");

        // 3. Assign role to a test staff user
        Role roleEntity = roleRepository.findById(createdRole.getRoleId()).orElseThrow();
        User staff = userRepository.save(User.builder()
                .fullName("Staff With Custom Role " + ts)
                .email("staff_custom_" + ts + "@store.com")
                .passwordHash("hashed")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(roleEntity)))
                .emailVerified(true)
                .provider(AuthProvider.LOCAL)
                .build());

        userRepository.flush();

        // 4. Try to delete role -> MUST throw BadRequestException because userCount == 1
        assertThatThrownBy(() -> roleService.deleteRole(createdRole.getRoleId()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("nhân sự sử dụng");

        // 5. Unassign role from staff
        staff.setRoles(new HashSet<>());
        userRepository.save(staff);
        userRepository.flush();

        // 6. Delete role again -> MUST succeed
        roleService.deleteRole(createdRole.getRoleId());
        roleRepository.flush();

        assertThat(roleRepository.findById(createdRole.getRoleId())).isEmpty();
    }

    @Test
    @DisplayName("System Protected Roles Guard: Cannot delete ROLE_ADMIN, ROLE_STAFF, or ROLE_CUSTOMER")
    void systemRoles_cannotBeDeleted() {
        Role admin = roleRepository.findByRoleName("ROLE_ADMIN").orElseThrow();
        Role staff = roleRepository.findByRoleName("ROLE_STAFF").orElseThrow();
        Role customer = roleRepository.findByRoleName("ROLE_CUSTOMER").orElseThrow();

        assertThatThrownBy(() -> roleService.deleteRole(admin.getRoleId()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("mặc định");

        assertThatThrownBy(() -> roleService.deleteRole(staff.getRoleId()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("mặc định");

        assertThatThrownBy(() -> roleService.deleteRole(customer.getRoleId()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("mặc định");
    }

    @Test
    @DisplayName("Admin Permission Depletion Guard: Cannot remove all permissions from ROLE_ADMIN")
    void adminPermissions_cannotBeDepleted() {
        Role admin = roleRepository.findByRoleName("ROLE_ADMIN").orElseThrow();

        RolePermissionsUpdateRequest emptyReq = RolePermissionsUpdateRequest.builder()
                .permissionCodes(Set.of())
                .build();

        assertThatThrownBy(() -> roleService.updateRolePermissions(admin.getRoleId(), emptyReq))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("toàn bộ quyền");
    }
}
