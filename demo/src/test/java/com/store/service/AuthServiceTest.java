package com.store.service;

import com.store.dto.request.LoginRequest;
import com.store.dto.request.RefreshTokenRequest;
import com.store.dto.request.RegisterRequest;
import com.store.dto.response.AuthResponse;
import com.store.entity.auth.AuthToken;
import com.store.entity.auth.TokenType;
import com.store.entity.user.Role;
import com.store.entity.user.User;
import com.store.entity.user.UserStatus;
import com.store.exception.DuplicateResourceException;
import com.store.repository.AuthTokenRepository;
import com.store.repository.RoleRepository;
import com.store.repository.UserRepository;
import com.store.security.CustomUserDetails;
import com.store.security.JwtTokenProvider;
import com.store.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private AuthTokenRepository authTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private com.store.security.LoginRateLimiter loginRateLimiter;

    @Mock
    private EmailService emailService;

    @Mock
    private OtpService otpService;

    @InjectMocks
    private AuthServiceImpl authService;

    private Role customerRole;
    private Role adminRole;
    private User customerUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        customerRole = Role.builder()
                .roleId(1)
                .roleName("ROLE_CUSTOMER")
                .description("Customer role")
                .build();

        adminRole = Role.builder()
                .roleId(2)
                .roleName("ROLE_ADMIN")
                .description("Admin role")
                .build();

        customerUser = User.builder()
                .userId(100L)
                .fullName("Nguyễn Văn A")
                .email("user@store.com")
                .phone("0987654321")
                .passwordHash("hashed_password")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(customerRole))
                .build();

        adminUser = User.builder()
                .userId(1L)
                .fullName("Administrator")
                .email("admin@store.com")
                .phone("0988888888")
                .passwordHash("hashed_admin_password")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(adminRole))
                .build();
    }

    @Nested
    @DisplayName("Register Tests")
    class RegisterTests {

        @Test
        @DisplayName("register should succeed when email is not taken")
        void register_success() {
            RegisterRequest request = RegisterRequest.builder()
                    .fullName("Nguyễn Văn A")
                    .email("user@store.com")
                    .phone("0987654321")
                    .password("Password@123")
                    .build();

            when(userRepository.existsByEmail("user@store.com")).thenReturn(false);
            when(userRepository.existsByPhone("0987654321")).thenReturn(false);
            when(roleRepository.findByRoleName("ROLE_CUSTOMER")).thenReturn(Optional.of(customerRole));
            when(passwordEncoder.encode("Password@123")).thenReturn("hashed_password");
            when(userRepository.save(any(User.class))).thenReturn(customerUser);
            when(jwtTokenProvider.generateAccessToken(any(CustomUserDetails.class))).thenReturn("mock_access_token");
            when(jwtTokenProvider.generateRefreshToken()).thenReturn("mock_refresh_token");
            when(jwtTokenProvider.getAccessTokenExpirationMs()).thenReturn(1800000L);

            AuthResponse response = authService.register(request);

            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("mock_access_token");
            assertThat(response.getRefreshToken()).isEqualTo("mock_refresh_token");
            assertThat(response.getUser().getEmail()).isEqualTo("user@store.com");
        }

        @Test
        @DisplayName("register should throw DuplicateResourceException when email is taken")
        void register_duplicateEmail_shouldThrow() {
            RegisterRequest request = RegisterRequest.builder()
                    .fullName("Nguyễn Văn A")
                    .email("user@store.com")
                    .password("Password@123")
                    .build();

            when(userRepository.existsByEmail("user@store.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("đã được đăng ký");
        }
    }

    @Nested
    @DisplayName("Customer & Admin Login Tests")
    class LoginTests {

        @Test
        @DisplayName("customerLogin should return tokens when credentials are valid")
        void customerLogin_success() {
            LoginRequest request = LoginRequest.builder()
                    .email("user@store.com")
                    .password("Password@123")
                    .build();

            when(userRepository.findByEmail("user@store.com")).thenReturn(Optional.of(customerUser));
            when(passwordEncoder.matches("Password@123", "hashed_password")).thenReturn(true);
            when(userRepository.save(any(User.class))).thenReturn(customerUser);
            when(jwtTokenProvider.generateAccessToken(any(CustomUserDetails.class))).thenReturn("mock_access_token");
            when(jwtTokenProvider.generateRefreshToken()).thenReturn("mock_refresh_token");

            AuthResponse response = authService.customerLogin(request);

            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("mock_access_token");
            assertThat(response.getUser().getFullName()).isEqualTo("Nguyễn Văn A");
        }

        @Test
        @DisplayName("customerLogin should throw IllegalArgumentException when password is wrong")
        void customerLogin_wrongPassword_shouldThrow() {
            LoginRequest request = LoginRequest.builder()
                    .email("user@store.com")
                    .password("WrongPassword")
                    .build();

            when(userRepository.findByEmail("user@store.com")).thenReturn(Optional.of(customerUser));
            when(passwordEncoder.matches("WrongPassword", "hashed_password")).thenReturn(false);

            assertThatThrownBy(() -> authService.customerLogin(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Email hoặc mật khẩu không chính xác");
        }

        @Test
        @DisplayName("adminLogin should succeed when user has ROLE_ADMIN")
        void adminLogin_success_whenAdmin() {
            LoginRequest request = LoginRequest.builder()
                    .email("admin@store.com")
                    .password("Admin@123456")
                    .build();

            when(userRepository.findByEmail("admin@store.com")).thenReturn(Optional.of(adminUser));
            when(passwordEncoder.matches("Admin@123456", "hashed_admin_password")).thenReturn(true);
            when(userRepository.save(any(User.class))).thenReturn(adminUser);
            when(jwtTokenProvider.generateAccessToken(any(CustomUserDetails.class))).thenReturn("admin_jwt_token");
            when(jwtTokenProvider.generateRefreshToken()).thenReturn("admin_refresh_token");

            AuthResponse response = authService.adminLogin(request);

            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("admin_jwt_token");
            assertThat(response.getUser().getRoles()).contains("ROLE_ADMIN");
        }

        @Test
        @DisplayName("adminLogin should throw AccessDeniedException when user is a customer")
        void adminLogin_forbidden_whenCustomer() {
            LoginRequest request = LoginRequest.builder()
                    .email("user@store.com")
                    .password("Password@123")
                    .build();

            when(userRepository.findByEmail("user@store.com")).thenReturn(Optional.of(customerUser));
            when(passwordEncoder.matches("Password@123", "hashed_password")).thenReturn(true);
            when(userRepository.save(any(User.class))).thenReturn(customerUser);

            assertThatThrownBy(() -> authService.adminLogin(request))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("Tài khoản không có quyền truy cập trang quản trị");
        }
    }

    @Nested
    @DisplayName("Refresh Token & Logout Tests")
    class TokenTests {

        @Test
        @DisplayName("refreshToken should generate new access token when valid")
        void refreshToken_success() {
            AuthToken authToken = AuthToken.builder()
                    .tokenId(1L)
                    .user(customerUser)
                    .token("valid_refresh_token")
                    .tokenType(TokenType.REFRESH_TOKEN)
                    .expiresAt(LocalDateTime.now().plusDays(5))
                    .build();

            RefreshTokenRequest request = RefreshTokenRequest.builder()
                    .refreshToken("valid_refresh_token")
                    .build();

            when(authTokenRepository.findByTokenAndTokenType(anyString(), org.mockito.ArgumentMatchers.eq(TokenType.REFRESH_TOKEN)))
                    .thenReturn(Optional.of(authToken));
            when(jwtTokenProvider.generateAccessToken(any(CustomUserDetails.class))).thenReturn("new_access_token");
            when(jwtTokenProvider.getAccessTokenExpirationMs()).thenReturn(1800000L);

            AuthResponse response = authService.refreshToken(request);

            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("new_access_token");
        }

        @Test
        @DisplayName("logout should delete hashed refresh token")
        void logout_success() {
            authService.logout("mock_refresh_token");

            verify(authTokenRepository).deleteByToken(anyString());
        }
    }

    @Nested
    @DisplayName("Forgot Password & Reset Password Tests")
    class ForgotPasswordTests {

        @Test
        @DisplayName("forgotPassword should generate OTP and send email when user exists and active")
        void forgotPassword_userExists_success() {
            com.store.dto.request.auth.ForgotPasswordRequest request = com.store.dto.request.auth.ForgotPasswordRequest.builder()
                    .email(customerUser.getEmail())
                    .build();

            when(userRepository.findByEmail(customerUser.getEmail())).thenReturn(Optional.of(customerUser));
            when(otpService.generateAndSaveOtp(customerUser.getEmail())).thenReturn("123456");

            authService.forgotPassword(request);

            verify(otpService).generateAndSaveOtp(customerUser.getEmail());
            verify(emailService).sendOtpEmail(customerUser.getEmail(), customerUser.getFullName(), "123456");
        }

        @Test
        @DisplayName("forgotPassword should silently return when user does not exist")
        void forgotPassword_userNotFound_silent() {
            com.store.dto.request.auth.ForgotPasswordRequest request = com.store.dto.request.auth.ForgotPasswordRequest.builder()
                    .email("unknown@store.com")
                    .build();

            when(userRepository.findByEmail("unknown@store.com")).thenReturn(Optional.empty());

            authService.forgotPassword(request);

            org.mockito.Mockito.verifyNoInteractions(otpService);
            org.mockito.Mockito.verifyNoInteractions(emailService);
        }

        @Test
        @DisplayName("verifyOtp should return reset token when OTP is valid")
        void verifyOtp_success() {
            com.store.dto.request.auth.VerifyOtpRequest request = com.store.dto.request.auth.VerifyOtpRequest.builder()
                    .email("customer@store.com")
                    .otp("123456")
                    .build();

            when(otpService.verifyOtpAndGenerateResetToken("customer@store.com", "123456")).thenReturn("uuid-reset-token");

            com.store.dto.response.auth.VerifyOtpResponse response = authService.verifyOtp(request);

            assertThat(response).isNotNull();
            assertThat(response.getResetToken()).isEqualTo("uuid-reset-token");
            assertThat(response.getEmail()).isEqualTo("customer@store.com");
        }

        @Test
        @DisplayName("resetPassword should update password and revoke tokens when token is valid")
        void resetPassword_success() {
            com.store.dto.request.auth.ResetPasswordRequest request = com.store.dto.request.auth.ResetPasswordRequest.builder()
                    .email(customerUser.getEmail())
                    .resetToken("uuid-reset-token")
                    .newPassword("newSecr3t!")
                    .confirmPassword("newSecr3t!")
                    .build();

            when(otpService.validateResetToken(customerUser.getEmail(), "uuid-reset-token")).thenReturn(true);
            when(userRepository.findByEmail(customerUser.getEmail())).thenReturn(Optional.of(customerUser));
            when(passwordEncoder.encode("newSecr3t!")).thenReturn("encoded_new_password");

            authService.resetPassword(request);

            verify(userRepository).save(customerUser);
            assertThat(customerUser.getPasswordHash()).isEqualTo("encoded_new_password");
            verify(authTokenRepository).deleteByUser_UserIdAndTokenType(customerUser.getUserId(), TokenType.REFRESH_TOKEN);
            verify(otpService).clearResetToken(customerUser.getEmail(), "uuid-reset-token");
        }

        @Test
        @DisplayName("resetPassword should throw exception when confirm password does not match")
        void resetPassword_passwordMismatch() {
            com.store.dto.request.auth.ResetPasswordRequest request = com.store.dto.request.auth.ResetPasswordRequest.builder()
                    .email("customer@store.com")
                    .resetToken("uuid-reset-token")
                    .newPassword("newSecr3t!")
                    .confirmPassword("differentPassword")
                    .build();

            assertThatThrownBy(() -> authService.resetPassword(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Mật khẩu xác nhận không khớp");
        }

        @Test
        @DisplayName("resetPassword should throw exception when reset token is invalid")
        void resetPassword_invalidToken() {
            com.store.dto.request.auth.ResetPasswordRequest request = com.store.dto.request.auth.ResetPasswordRequest.builder()
                    .email("customer@store.com")
                    .resetToken("expired-token")
                    .newPassword("newSecr3t!")
                    .confirmPassword("newSecr3t!")
                    .build();

            when(otpService.validateResetToken("customer@store.com", "expired-token")).thenReturn(false);

            assertThatThrownBy(() -> authService.resetPassword(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Phiên đặt lại mật khẩu không hợp lệ");
        }
    }
}
