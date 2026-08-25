package com.store.service;

import com.store.dto.response.PageResponse;
import com.store.dto.user.admin.AdminUserCreateRequest;
import com.store.dto.user.admin.AdminUserPasswordResetRequest;
import com.store.dto.user.admin.AdminUserResponse;
import com.store.dto.user.admin.AdminUserStatusRequest;
import com.store.dto.user.admin.AdminUserUpdateRequest;
import com.store.dto.user.admin.RoleResponse;

import java.util.List;

public interface AdminUserService {

    PageResponse<AdminUserResponse> getUsersPaginated(
            int page, int size, String keyword, String role, String status,
            String sortBy, String sortDir
    );

    PageResponse<AdminUserResponse> getCustomersPaginated(
            int page, int size, String keyword, String status,
            String sortBy, String sortDir
    );

    PageResponse<AdminUserResponse> getStaffPaginated(
            int page, int size, String keyword, String role, String status,
            String sortBy, String sortDir
    );

    AdminUserResponse getUserById(Long userId);

    AdminUserResponse createUser(AdminUserCreateRequest request);

    AdminUserResponse updateUser(Long userId, AdminUserUpdateRequest request, Long currentUserId);

    AdminUserResponse updateUserStatus(Long userId, AdminUserStatusRequest request, Long currentUserId);

    void resetUserPassword(Long userId, AdminUserPasswordResetRequest request);

    void deleteUser(Long userId, Long currentUserId);

    List<RoleResponse> getAllRoles();
}
