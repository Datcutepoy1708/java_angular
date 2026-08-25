package com.store.service.impl;

import com.store.dto.response.PageResponse;
import com.store.dto.user.admin.AdminUserCreateRequest;
import com.store.dto.user.admin.AdminUserPasswordResetRequest;
import com.store.dto.user.admin.AdminUserResponse;
import com.store.dto.user.admin.AdminUserStatusRequest;
import com.store.dto.user.admin.AdminUserUpdateRequest;
import com.store.dto.user.admin.RoleResponse;
import com.store.entity.user.AuthProvider;
import com.store.entity.user.Gender;
import com.store.entity.user.Role;
import com.store.entity.user.User;
import com.store.entity.user.UserStatus;
import com.store.exception.BadRequestException;
import com.store.exception.DuplicateResourceException;
import com.store.exception.ResourceNotFoundException;
import com.store.repository.OrderRepository;
import com.store.repository.RoleRepository;
import com.store.repository.UserRepository;
import com.store.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final OrderRepository orderRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminUserResponse> getUsersPaginated(
            int page, int size, String keyword, String role, String status,
            String sortBy, String sortDir
    ) {
        Sort sort = "asc".equalsIgnoreCase(sortDir)
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        String kw = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        String roleFilter = (role != null && !role.isBlank() && !"all".equalsIgnoreCase(role)) ? role.trim() : null;
        UserStatus st = null;
        if (status != null && !status.isBlank() && !"all".equalsIgnoreCase(status)) {
            try {
                st = UserStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }

        Page<User> userPage = userRepository.findAllFiltered(kw, roleFilter, st, pageable);
        List<AdminUserResponse> content = userPage.getContent()
                .stream()
                .map(this::mapToAdminUserResponse)
                .toList();

        return PageResponse.<AdminUserResponse>builder()
                .content(content)
                .pageNumber(userPage.getNumber())
                .pageSize(userPage.getSize())
                .totalElements(userPage.getTotalElements())
                .totalPages(userPage.getTotalPages())
                .last(userPage.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminUserResponse> getCustomersPaginated(
            int page, int size, String keyword, String status,
            String sortBy, String sortDir
    ) {
        Sort sort = "asc".equalsIgnoreCase(sortDir)
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        String kw = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        UserStatus st = null;
        if (status != null && !status.isBlank() && !"all".equalsIgnoreCase(status)) {
            try {
                st = UserStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }

        Page<User> userPage = userRepository.findAllCustomersFiltered(kw, st, pageable);
        List<AdminUserResponse> content = userPage.getContent()
                .stream()
                .map(this::mapToAdminUserResponse)
                .toList();

        return PageResponse.<AdminUserResponse>builder()
                .content(content)
                .pageNumber(userPage.getNumber())
                .pageSize(userPage.getSize())
                .totalElements(userPage.getTotalElements())
                .totalPages(userPage.getTotalPages())
                .last(userPage.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminUserResponse> getStaffPaginated(
            int page, int size, String keyword, String role, String status,
            String sortBy, String sortDir
    ) {
        Sort sort = "asc".equalsIgnoreCase(sortDir)
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        String kw = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        String roleFilter = (role != null && !role.isBlank() && !"all".equalsIgnoreCase(role)) ? role.trim() : null;
        UserStatus st = null;
        if (status != null && !status.isBlank() && !"all".equalsIgnoreCase(status)) {
            try {
                st = UserStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }

        Page<User> userPage = userRepository.findAllStaffFiltered(kw, roleFilter, st, pageable);
        List<AdminUserResponse> content = userPage.getContent()
                .stream()
                .map(this::mapToAdminUserResponse)
                .toList();

        return PageResponse.<AdminUserResponse>builder()
                .content(content)
                .pageNumber(userPage.getNumber())
                .pageSize(userPage.getSize())
                .totalElements(userPage.getTotalElements())
                .totalPages(userPage.getTotalPages())
                .last(userPage.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AdminUserResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại với ID: " + userId));
        return mapToAdminUserResponse(user);
    }

    @Override
    @Transactional
    public AdminUserResponse createUser(AdminUserCreateRequest request) {
        log.info("Admin creating new user/staff with email: {}", request.getEmail());
        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email '" + email + "' đã được sử dụng trong hệ thống");
        }

        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            String phone = request.getPhone().trim();
            if (userRepository.existsByPhone(phone)) {
                throw new DuplicateResourceException("Số điện thoại '" + phone + "' đã được sử dụng");
            }
        }

        Set<Role> roles = resolveRoles(request.getRoles());
        if (roles.isEmpty()) {
            Role staffRole = roleRepository.findByRoleName("ROLE_STAFF")
                    .orElseThrow(() -> new ResourceNotFoundException("Role ROLE_STAFF không tồn tại"));
            roles.add(staffRole);
        }

        Gender gender = parseGender(request.getGender());
        UserStatus status = parseStatus(request.getStatus());

        User user = User.builder()
                .fullName(request.getFullName().trim())
                .email(email)
                .phone(request.getPhone() != null && !request.getPhone().isBlank() ? request.getPhone().trim() : null)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .gender(gender)
                .birthDate(request.getBirthDate())
                .status(status)
                .emailVerified(true)
                .provider(AuthProvider.LOCAL)
                .roles(roles)
                .build();

        User saved = userRepository.save(user);
        log.info("User created successfully with ID: {}", saved.getUserId());
        return mapToAdminUserResponse(saved);
    }

    @Override
    @Transactional
    public AdminUserResponse updateUser(Long userId, AdminUserUpdateRequest request, Long currentUserId) {
        log.info("Admin updating user ID: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại với ID: " + userId));

        // 1. Check unique phone
        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            String phone = request.getPhone().trim();
            if (userRepository.existsByPhoneAndUserIdNot(phone, userId)) {
                throw new DuplicateResourceException("Số điện thoại '" + phone + "' đã được sử dụng bởi tài khoản khác");
            }
        }

        // 2. Pre-mutation Validation Pattern for Active Admin Guard
        boolean isCurrentlyActiveAdmin = user.getStatus() == UserStatus.ACTIVE &&
                user.getRoles().stream().anyMatch(r -> "ROLE_ADMIN".equals(r.getRoleName()));

        boolean requestedKeepsAdmin = request.getRoles() != null &&
                request.getRoles().stream().anyMatch(r -> "ROLE_ADMIN".equalsIgnoreCase(r) || "ADMIN".equalsIgnoreCase(r));
        boolean requestedIsActive = "active".equalsIgnoreCase(request.getStatus());

        boolean losesActiveAdminState = isCurrentlyActiveAdmin && (!requestedKeepsAdmin || !requestedIsActive);

        if (losesActiveAdminState) {
            // Self-operation check
            if (user.getUserId().equals(currentUserId)) {
                throw new BadRequestException("Không thể tự gỡ vai trò Quản trị viên hoặc tự khóa tài khoản của chính mình");
            }
            // Count active admins in DB before mutating entity
            long activeAdminCount = userRepository.countActiveAdmins();
            if (activeAdminCount <= 1) {
                throw new BadRequestException("Không thể gỡ vai trò Quản trị viên hoặc khóa tài khoản của Admin đang hoạt động cuối cùng trong hệ thống");
            }
        }

        // 3. Mutate entity fields after all validation passes
        Set<Role> roles = resolveRoles(request.getRoles());
        user.setFullName(request.getFullName().trim());
        user.setPhone(request.getPhone() != null && !request.getPhone().isBlank() ? request.getPhone().trim() : null);
        user.setGender(parseGender(request.getGender()));
        user.setBirthDate(request.getBirthDate());
        user.setStatus(parseStatus(request.getStatus()));
        user.setRoles(roles);

        User updated = userRepository.save(user);
        log.info("User ID: {} updated successfully", updated.getUserId());
        return mapToAdminUserResponse(updated);
    }

    @Override
    @Transactional
    public AdminUserResponse updateUserStatus(Long userId, AdminUserStatusRequest request, Long currentUserId) {
        log.info("Admin updating status for user ID: {} to {}", userId, request.getStatus());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại với ID: " + userId));

        UserStatus newStatus = parseStatus(request.getStatus());

        // Pre-mutation Validation Pattern for Active Admin Guard
        boolean isCurrentlyActiveAdmin = user.getStatus() == UserStatus.ACTIVE &&
                user.getRoles().stream().anyMatch(r -> "ROLE_ADMIN".equals(r.getRoleName()));

        if (isCurrentlyActiveAdmin && newStatus != UserStatus.ACTIVE) {
            if (user.getUserId().equals(currentUserId)) {
                throw new BadRequestException("Không thể tự khóa hoặc tạm ngưng tài khoản của chính mình");
            }
            long activeAdminCount = userRepository.countActiveAdmins();
            if (activeAdminCount <= 1) {
                throw new BadRequestException("Không thể khóa hoặc tạm ngưng tài khoản Quản trị viên đang hoạt động cuối cùng của hệ thống");
            }
        }

        user.setStatus(newStatus);
        User updated = userRepository.save(user);
        return mapToAdminUserResponse(updated);
    }

    @Override
    @Transactional
    public void resetUserPassword(Long userId, AdminUserPasswordResetRequest request) {
        log.info("Admin resetting password for user ID: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại với ID: " + userId));

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Password successfully reset for user ID: {}", userId);
    }

    @Override
    @Transactional
    public void deleteUser(Long userId, Long currentUserId) {
        log.info("Admin deleting user ID: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại với ID: " + userId));

        if (user.getUserId().equals(currentUserId)) {
            throw new BadRequestException("Không thể tự xóa tài khoản của chính mình");
        }

        boolean isCurrentlyActiveAdmin = user.getStatus() == UserStatus.ACTIVE &&
                user.getRoles().stream().anyMatch(r -> "ROLE_ADMIN".equals(r.getRoleName()));

        if (isCurrentlyActiveAdmin) {
            long activeAdminCount = userRepository.countActiveAdmins();
            if (activeAdminCount <= 1) {
                throw new BadRequestException("Không thể xóa tài khoản Quản trị viên đang hoạt động cuối cùng của hệ thống");
            }
        }

        userRepository.delete(user);
        log.info("User ID: {} deleted successfully", userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleResponse> getAllRoles() {
        return roleRepository.findAllByOrderByRoleIdAsc()
                .stream()
                .map(RoleResponse::fromEntity)
                .toList();
    }

    // ── Helper Methods ─────────────────────────────────────────────
    private AdminUserResponse mapToAdminUserResponse(User user) {
        long totalOrders = orderRepository.countByUserUserId(user.getUserId());
        BigDecimal totalSpend = orderRepository.sumTotalSpendByUserId(user.getUserId());
        return AdminUserResponse.fromEntity(user, totalOrders, totalSpend);
    }

    private Set<Role> resolveRoles(Set<String> roleNames) {
        Set<Role> roles = new HashSet<>();
        if (roleNames == null || roleNames.isEmpty()) {
            return roles;
        }

        for (String name : roleNames) {
            if (name == null || name.isBlank()) continue;
            String normalized = name.trim().toUpperCase();
            if (!normalized.startsWith("ROLE_")) {
                normalized = "ROLE_" + normalized;
            }
            Role role = roleRepository.findByRoleName(normalized)
                    .orElseThrow(() -> new ResourceNotFoundException("Vai trò không tồn tại: " + name));
            roles.add(role);
        }
        return roles;
    }

    private Gender parseGender(String genderStr) {
        if (genderStr == null || genderStr.isBlank()) return null;
        try {
            return Gender.valueOf(genderStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private UserStatus parseStatus(String statusStr) {
        if (statusStr == null || statusStr.isBlank()) return UserStatus.ACTIVE;
        try {
            return UserStatus.valueOf(statusStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return UserStatus.ACTIVE;
        }
    }
}
