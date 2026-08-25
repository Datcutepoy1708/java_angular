package com.store.dto.role;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RolePermissionsUpdateRequest {

    @NotNull(message = "Danh sách mã quyền không được để null")
    @Builder.Default
    private Set<String> permissionCodes = new HashSet<>();
}
