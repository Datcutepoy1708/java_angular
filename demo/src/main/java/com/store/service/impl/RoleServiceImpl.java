package com.store.service.impl;

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
import com.store.service.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    public static final Set<String> SYSTEM_ROLES = Set.of("ROLE_ADMIN", "ROLE_STAFF", "ROLE_CUSTOMER");
    public static final Set<String> CRITICAL_ADMIN_PERMISSIONS = Set.of("ROLE_MANAGE", "STAFF_MANAGE", "SETTING_MANAGE");

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "roles", key = "'all'")
    public List<RoleDetailResponse> getAllRoles() {
        log.info("Fetching all roles from database (cache miss)");
        List<Role> roles = roleRepository.findAllByOrderByRoleIdAsc();
        return roles.stream()
                .map(this::mapToDetailResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public RoleDetailResponse getRoleById(Integer roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chức vụ với ID: " + roleId));
        return mapToDetailResponse(role);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "roles", allEntries = true)
    public RoleDetailResponse createRole(RoleCreateRequest request) {
        String normalizedCode = normalizeRoleCode(request.getRoleName());
        log.info("Creating new role with code: {}", normalizedCode);

        if (roleRepository.existsByRoleName(normalizedCode)) {
            throw new DuplicateResourceException("Mã chức vụ '" + normalizedCode + "' đã tồn tại trong hệ thống");
        }

        Set<Permission> permissions = resolvePermissions(request.getPermissionCodes());

        Role role = Role.builder()
                .roleName(normalizedCode)
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .permissions(permissions)
                .build();

        Role saved = roleRepository.save(role);
        log.info("Role created successfully with ID: {}", saved.getRoleId());
        return mapToDetailResponse(saved);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "roles", allEntries = true)
    public RoleDetailResponse updateRole(Integer roleId, RoleUpdateRequest request) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chức vụ với ID: " + roleId));

        boolean isSystemRole = isSystemRole(role.getRoleName());

        if (request.getRoleName() != null && !request.getRoleName().isBlank()) {
            String newCode = normalizeRoleCode(request.getRoleName());
            if (!newCode.equals(role.getRoleName())) {
                if (isSystemRole) {
                    throw new BadRequestException("Không thể thay đổi mã của chức vụ hệ thống mặc định: " + role.getRoleName());
                }
                if (roleRepository.existsByRoleNameAndRoleIdNot(newCode, roleId)) {
                    throw new DuplicateResourceException("Mã chức vụ '" + newCode + "' đã tồn tại trong hệ thống");
                }
                role.setRoleName(newCode);
            }
        }

        if (request.getDescription() != null) {
            role.setDescription(request.getDescription().trim());
        }

        Role updated = roleRepository.save(role);
        log.info("Role ID {} updated successfully", roleId);
        return mapToDetailResponse(updated);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "roles", allEntries = true)
    public void deleteRole(Integer roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chức vụ với ID: " + roleId));

        if (isSystemRole(role.getRoleName())) {
            throw new BadRequestException("Không thể xóa vai trò hệ thống mặc định: " + role.getRoleName());
        }

        long userCount = userRepository.countByRoles_RoleId(roleId);
        if (userCount > 0) {
            throw new BadRequestException("Không thể xóa vai trò '" + role.getRoleName() +
                    "' đang có " + userCount + " nhân sự sử dụng. Vui lòng chuyển đổi vai trò cho nhân sự trước.");
        }

        roleRepository.delete(role);
        log.info("Role ID {} deleted successfully", roleId);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "roles", allEntries = true)
    public RoleDetailResponse updateRolePermissions(Integer roleId, RolePermissionsUpdateRequest request) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chức vụ với ID: " + roleId));

        Set<String> requestedCodes = request.getPermissionCodes() != null ? request.getPermissionCodes() : new HashSet<>();

        // Admin Permission Depletion Guard
        if ("ROLE_ADMIN".equals(role.getRoleName())) {
            if (requestedCodes.isEmpty()) {
                throw new BadRequestException("Không thể gỡ bỏ toàn bộ quyền của vai trò Quản trị viên tối cao (ROLE_ADMIN)");
            }
            for (String critical : CRITICAL_ADMIN_PERMISSIONS) {
                if (!requestedCodes.contains(critical)) {
                    throw new BadRequestException("Không thể gỡ bỏ quyền quản trị cốt lõi '" + critical + "' khỏi vai trò ROLE_ADMIN");
                }
            }
        }

        Set<Permission> permissions = resolvePermissions(requestedCodes);
        role.setPermissions(permissions);
        Role updated = roleRepository.save(role);
        log.info("Permissions updated for role ID {}", roleId);
        return mapToDetailResponse(updated);
    }

    private RoleDetailResponse mapToDetailResponse(Role role) {
        long userCount = userRepository.countByRoles_RoleId(role.getRoleId());
        Set<String> permissionCodes = role.getPermissions().stream()
                .map(Permission::getPermissionCode)
                .collect(Collectors.toSet());

        return RoleDetailResponse.builder()
                .roleId(role.getRoleId())
                .roleName(role.getRoleName())
                .description(role.getDescription())
                .isSystemRole(isSystemRole(role.getRoleName()))
                .userCount(userCount)
                .permissionCodes(permissionCodes)
                .createdAt(role.getCreatedAt())
                .build();
    }

    private boolean isSystemRole(String roleName) {
        return roleName != null && SYSTEM_ROLES.contains(roleName.toUpperCase());
    }

    private String normalizeRoleCode(String input) {
        if (input == null || input.isBlank()) {
            throw new BadRequestException("Mã chức vụ không được để trống");
        }
        String upper = input.trim().toUpperCase().replaceAll("\\s+", "_");
        if (!upper.startsWith("ROLE_")) {
            upper = "ROLE_" + upper;
        }
        return upper;
    }

    private Set<Permission> resolvePermissions(Set<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return new HashSet<>();
        }
        Set<Permission> permissions = new HashSet<>();
        for (String code : codes) {
            if (code != null && !code.isBlank()) {
                String trimmed = code.trim().toUpperCase();
                Permission perm = permissionRepository.findByPermissionCode(trimmed)
                        .orElseGet(() -> permissionRepository.save(
                                Permission.builder()
                                        .permissionCode(trimmed)
                                        .description("Quyền " + trimmed)
                                        .build()
                        ));
                permissions.add(perm);
            }
        }
        return permissions;
    }
}
