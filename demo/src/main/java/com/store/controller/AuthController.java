package com.store.controller;

import com.store.dto.request.LoginRequest;
import com.store.dto.request.RefreshTokenRequest;
import com.store.dto.request.RegisterRequest;
import com.store.dto.response.ApiResponse;
import com.store.dto.response.AuthResponse;
import com.store.dto.response.UserSummaryResponse;
import com.store.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication & Authorization", description = "Customer & Admin Authentication, Token Refresh, and Profile APIs")
public class AuthController {

    private final AuthService authService;

    @Value("${app.oauth2.google.client-id:}")
    private String googleClientId;

    @Value("${app.oauth2.facebook.app-id:}")
    private String facebookAppId;

    @PostMapping("/register")
    @Operation(summary = "Customer Registration", description = "Register a new customer account with default ROLE_CUSTOMER")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Đăng ký tài khoản thành công", response));
    }

    @PostMapping("/login")
    @Operation(summary = "Customer Login (Storefront)", description = "Authenticate customer credentials and return JWT Access + Refresh token")
    public ResponseEntity<ApiResponse<AuthResponse>> customerLogin(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.customerLogin(request);
        return ResponseEntity.ok(ApiResponse.success("Đăng nhập thành công", response));
    }

    @PostMapping("/admin/login")
    @Operation(summary = "Admin & Staff Portal Login", description = "Dedicated login endpoint for administrators and staff with strict RBAC check")
    public ResponseEntity<ApiResponse<AuthResponse>> adminLogin(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.adminLogin(request);
        return ResponseEntity.ok(ApiResponse.success("Đăng nhập trang quản trị thành công", response));
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "Refresh Access Token", description = "Exchange a valid refresh token for a newly generated access token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success("Làm mới token thành công", response));
    }

    @GetMapping("/me")
    @Operation(summary = "Get Current User Profile", description = "Retrieve profile, roles, and permissions of the currently authenticated user")
    public ResponseEntity<ApiResponse<UserSummaryResponse>> getCurrentUser() {
        UserSummaryResponse response = authService.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin tài khoản thành công", response));
    }

    @PostMapping("/logout")
    @Operation(summary = "User Logout", description = "Revoke the refresh token from active token store")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestBody(required = false) RefreshTokenRequest request) {
        if (request != null) {
            authService.logout(request.getRefreshToken());
        }
        return ResponseEntity.ok(ApiResponse.success("Đăng xuất thành công", null));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Forgot Password - Request OTP", description = "Send 6-digit OTP to user's registered email address")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody com.store.dto.request.auth.ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Nếu email tồn tại trên hệ thống, mã OTP đã được gửi đến hộp thư của bạn.", null));
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Forgot Password - Verify OTP", description = "Verify 6-digit OTP and return a one-time password reset token")
    public ResponseEntity<ApiResponse<com.store.dto.response.auth.VerifyOtpResponse>> verifyOtp(@Valid @RequestBody com.store.dto.request.auth.VerifyOtpRequest request) {
        com.store.dto.response.auth.VerifyOtpResponse response = authService.verifyOtp(request);
        return ResponseEntity.ok(ApiResponse.success("Xác thực mã OTP thành công", response));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Forgot Password - Reset Password", description = "Set a new password using the validated reset token")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody com.store.dto.request.auth.ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Đặt lại mật khẩu thành công. Vui lòng đăng nhập bằng mật khẩu mới.", null));
    }

    @PostMapping("/social/google")
    @Operation(summary = "Google Social Login", description = "Authenticate using Google ID Token and receive application JWT tokens")
    public ResponseEntity<ApiResponse<AuthResponse>> loginWithGoogle(
            @Valid @RequestBody com.store.dto.request.auth.GoogleLoginRequest request,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        AuthResponse response = authService.loginWithGoogle(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Đăng nhập Google thành công", response));
    }

    @PostMapping("/social/facebook")
    @Operation(summary = "Facebook Social Login", description = "Authenticate using Facebook Access Token and receive application JWT tokens")
    public ResponseEntity<ApiResponse<AuthResponse>> loginWithFacebook(
            @Valid @RequestBody com.store.dto.request.auth.FacebookLoginRequest request,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        AuthResponse response = authService.loginWithFacebook(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Đăng nhập Facebook thành công", response));
    }

    @GetMapping("/oauth2/config")
    @Operation(summary = "Get Public OAuth2 Client Config", description = "Get public Google and Facebook client IDs loaded from environment (.env)")
    public ResponseEntity<ApiResponse<Map<String, String>>> getOAuth2Config() {
        return ResponseEntity.ok(ApiResponse.success("OAuth2 configuration retrieved", Map.of(
                "googleClientId", googleClientId != null ? googleClientId : "",
                "facebookAppId", facebookAppId != null ? facebookAppId : ""
        )));
    }
}
