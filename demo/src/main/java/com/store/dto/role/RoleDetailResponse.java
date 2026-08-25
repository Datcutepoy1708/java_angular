package com.store.dto.role;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleDetailResponse {

    private Integer roleId;
    private String roleName;
    private String description;
    private boolean isSystemRole;
    private long userCount;
    @Builder.Default
    private Set<String> permissionCodes = new HashSet<>();
    private LocalDateTime createdAt;
}
