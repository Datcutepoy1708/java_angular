package com.store.dto.role;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleUpdateRequest {

    @Size(max = 50, message = "Mã chức vụ không được vượt quá 50 ký tự")
    private String roleName;

    @Size(max = 255, message = "Mô tả không được vượt quá 255 ký tự")
    private String description;
}
