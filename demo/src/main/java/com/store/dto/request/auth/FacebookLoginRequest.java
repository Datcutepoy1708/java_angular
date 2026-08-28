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
@Schema(description = "Request body cho xác thực đăng nhập bằng Facebook")
public class FacebookLoginRequest {

    @NotBlank(message = "Facebook Access Token không được để trống")
    @Schema(description = "User Access Token được trả về từ Facebook Javascript SDK (FB.login)", requiredMode = Schema.RequiredMode.REQUIRED)
    private String accessToken;
}
