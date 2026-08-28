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
@Schema(description = "Request body cho xác thực đăng nhập bằng Zalo OAuth v4 PKCE")
public class ZaloLoginRequest {

    @NotBlank(message = "Authorization code không được để trống")
    @Schema(description = "Mã Authorization Code nhận được từ Zalo OAuth popup", example = "zalo_auth_code_example")
    private String code;

    @NotBlank(message = "Code verifier không được để trống")
    @Schema(description = "Mã Code Verifier sinh ra bởi client dùng cho quy trình PKCE", example = "random_code_verifier_string")
    private String codeVerifier;
}
