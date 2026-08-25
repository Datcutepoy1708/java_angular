package com.store.dto.role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
public class RoleCreateRequest {

    @NotBlank(message = "Tên/Mã chức vụ không được để trống")
    @Size(max = 50, message = "Mã chức vụ không được vượt quá 50 ký tự")
    @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "Mã chức vụ chỉ được chứa chữ cái, số và dấu gạch dưới")
    private String roleName;

    @Size(max = 255, message = "Mô tả không được vượt quá 255 ký tự")
    private String description;

    @Builder.Default
    private Set<String> permissionCodes = new HashSet<>();
}
