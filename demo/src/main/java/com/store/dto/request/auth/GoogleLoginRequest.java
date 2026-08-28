package com.store.dto.request.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request body cho xác thực đăng nhập bằng Google")
public class GoogleLoginRequest {

    @NotBlank(message = "Google ID Token không được để trống")
    @Schema(description = "ID Token (JWT) được trả về từ Google Identity Services (GIS)", requiredMode = Schema.RequiredMode.REQUIRED)
    private String idToken;
}
