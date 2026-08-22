package com.store.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.store.entity.user.Permission;
import com.store.entity.user.Role;
import com.store.entity.user.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserSummaryResponse implements Serializable {

    private Long userId;
    private String fullName;
    private String email;
    private String phone;
    private String avatarUrl;
    private String status;
    private List<String> roles;
    private List<String> permissions;

    public static UserSummaryResponse fromEntity(User user) {
        if (user == null) {
            return null;
        }

        List<String> roles = null;
        List<String> permissions = null;

        if (user.getRoles() != null) {
            roles = user.getRoles().stream()
                    .map(Role::getRoleName)
                    .toList();

            permissions = user.getRoles().stream()
                    .filter(r -> r.getPermissions() != null)
                    .flatMap(r -> r.getPermissions().stream())
                    .map(Permission::getPermissionCode)
                    .distinct()
                    .toList();
        }

        return UserSummaryResponse.builder()
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .status(user.getStatus() != null ? user.getStatus().getValue() : null)
                .roles(roles)
                .permissions(permissions)
                .build();
    }
}
