package com.store.dto.user.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminUserStatusRequest {

    @NotBlank(message = "Trạng thái không được để trống")
    private String status; // active, inactive, banned
}
