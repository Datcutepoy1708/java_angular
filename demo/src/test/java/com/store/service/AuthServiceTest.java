package com.store.service;

import com.store.dto.request.LoginRequest;
import com.store.dto.request.RefreshTokenRequest;
import com.store.dto.request.RegisterRequest;
import com.store.dto.request.auth.FacebookLoginRequest;
import com.store.dto.request.auth.ForgotPasswordRequest;
import com.store.dto.request.auth.GoogleLoginRequest;
import com.store.dto.request.auth.ResetPasswordRequest;
import com.store.dto.request.auth.VerifyOtpRequest;
import com.store.dto.request.auth.ZaloLoginRequest;
import com.store.dto.response.AuthResponse;
import com.store.dto.response.auth.SocialUserInfo;
import com.store.dto.response.auth.VerifyOtpResponse;
import com.store.entity.auth.AuthToken;
import com.store.entity.auth.TokenType;
import com.store.entity.user.AuthProvider;
import com.store.entity.user.Role;
import com.store.entity.user.User;
import com.store.entity.user.UserStatus;
import com.store.event.SocialLoginPostProcessEvent;
import com.store.exception.DuplicateResourceException;
import com.store.repository.AuthTokenRepository;
import com.store.repository.OrderRepository;
import com.store.repository.RoleRepository;
import com.store.repository.UserRepository;
import com.store.security.CustomUserDetails;
import com.store.security.JwtTokenProvider;
import com.store.security.LoginRateLimiter;
import com.store.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
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
    private LoginRateLimiter loginRateLimiter;

    @Mock
    private EmailService emailService;

    @Mock
    private OtpService otpService;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private SocialAuthService socialAuthService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

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
                .email("customer@store.com")
                .fullName("Customer User")
                .passwordHash("$2a$10$hashedPassword")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(customerRole))
                .build();

        adminUser = User.builder()
                .userId(101L)
                .email("admin@store.com")
                .fullName("Admin User")
                .passwordHash("$2a$10$hashedAdminPassword")
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
                    .fullName("New User")
                    .email("new@store.com")
                    .password("Password123!")
                    .phone("0901234567")
                    .build();

            when(userRepository.existsByEmail("new@store.com")).thenReturn(false);
            when(userRepository.existsByPhone("0901234567")).thenReturn(false);
            when(roleRepository.findByRoleName("ROLE_CUSTOMER")).thenReturn(Optional.of(customerRole));
            when(passwordEncoder.encode("Password123!")).thenReturn("$2a$10$encodedPassword");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User u = invocation.getArgument(0);
                u.setUserId(200L);
                return u;
            });
            when(jwtTokenProvider.generateAccessToken(any(CustomUserDetails.class))).thenReturn("access-token-123");
            when(jwtTokenProvider.generateRefreshToken()).thenReturn("refresh-token-456");

            AuthResponse response = authService.register(request);

            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("access-token-123");
            assertThat(response.getRefreshToken()).isEqualTo("refresh-token-456");
            assertThat(response.getUser().getEmail()).isEqualTo("new@store.com");
            verify(authTokenRepository).save(any(AuthToken.class));
        }

        @Test
        @DisplayName("register should throw exception when email is already registered")
        void register_duplicateEmail() {
            RegisterRequest request = RegisterRequest.builder()
                    .fullName("Duplicate User")
                    .email("existing@store.com")
                    .password("Password123!")
                    .build();

            when(userRepository.existsByEmail("existing@store.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("đã được đăng ký");
        }
    }

    @Nested
    @DisplayName("Login Tests")
    class LoginTests {

        @Test
        @DisplayName("customerLogin should succeed with correct credentials")
        void customerLogin_success() {
            LoginRequest request = LoginRequest.builder()
                    .email("customer@store.com")
                    .password("Password123!")
                    .build();

            when(userRepository.findByEmail("customer@store.com")).thenReturn(Optional.of(customerUser));
            when(passwordEncoder.matches("Password123!", "$2a$10$hashedPassword")).thenReturn(true);
            when(userRepository.save(any(User.class))).thenReturn(customerUser);
            when(jwtTokenProvider.generateAccessToken(any(CustomUserDetails.class))).thenReturn("access-token-123");
            when(jwtTokenProvider.generateRefreshToken()).thenReturn("refresh-token-456");

            AuthResponse response = authService.customerLogin(request);

            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("access-token-123");
            verify(loginRateLimiter).resetAttempts("customer@store.com", null);
        }

        @Test
        @DisplayName("customerLogin should throw exception on wrong password")
        void customerLogin_wrongPassword() {
            LoginRequest request = LoginRequest.builder()
                    .email("customer@store.com")
                    .password("WrongPassword")
                    .build();

            when(userRepository.findByEmail("customer@store.com")).thenReturn(Optional.of(customerUser));
            when(passwordEncoder.matches("WrongPassword", "$2a$10$hashedPassword")).thenReturn(false);

            assertThatThrownBy(() -> authService.customerLogin(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Email hoặc mật khẩu không chính xác");

            verify(loginRateLimiter).recordFailedAttempt("customer@store.com", null);
        }

        @Test
        @DisplayName("adminLogin should succeed when user has ROLE_ADMIN")
        void adminLogin_success() {
            LoginRequest request = LoginRequest.builder()
                    .email("admin@store.com")
                    .password("AdminPass123!")
                    .build();

            when(userRepository.findByEmail("admin@store.com")).thenReturn(Optional.of(adminUser));
            when(passwordEncoder.matches("AdminPass123!", "$2a$10$hashedAdminPassword")).thenReturn(true);
            when(userRepository.save(any(User.class))).thenReturn(adminUser);
            when(jwtTokenProvider.generateAccessToken(any(CustomUserDetails.class))).thenReturn("admin-access-token");
            when(jwtTokenProvider.generateRefreshToken()).thenReturn("admin-refresh-token");

            AuthResponse response = authService.adminLogin(request);

            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("admin-access-token");
            assertThat(response.getUser().getRoles()).contains("ROLE_ADMIN");
        }

        @Test
        @DisplayName("adminLogin should throw AccessDeniedException when customer attempts admin login")
        void adminLogin_accessDenied() {
            LoginRequest request = LoginRequest.builder()
                    .email("customer@store.com")
                    .password("Password123!")
                    .build();

            when(userRepository.findByEmail("customer@store.com")).thenReturn(Optional.of(customerUser));
            when(passwordEncoder.matches("Password123!", "$2a$10$hashedPassword")).thenReturn(true);
            when(userRepository.save(any(User.class))).thenReturn(customerUser);

            assertThatThrownBy(() -> authService.adminLogin(request))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("không có quyền truy cập");
        }
    }

    @Nested
    @DisplayName("Token Tests")
    class TokenTests {

        @Test
        @DisplayName("refreshToken should succeed with valid unexpired token")
        void refreshToken_success() {
            RefreshTokenRequest request = RefreshTokenRequest.builder()
                    .refreshToken("raw-refresh-token")
                    .build();

            AuthToken validToken = AuthToken.builder()
                    .tokenId(1L)
                    .user(customerUser)
                    .tokenType(TokenType.REFRESH_TOKEN)
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .build();

            when(authTokenRepository.findByTokenAndTokenType(anyString(), any(TokenType.class)))
                    .thenReturn(Optional.of(validToken));
            when(jwtTokenProvider.generateAccessToken(any(CustomUserDetails.class)))
                    .thenReturn("new-access-token");

            AuthResponse response = authService.refreshToken(request);

            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("new-access-token");
            assertThat(response.getRefreshToken()).isEqualTo("raw-refresh-token");
        }

        @Test
        @DisplayName("refreshToken should throw exception when token is expired")
        void refreshToken_expired() {
            RefreshTokenRequest request = RefreshTokenRequest.builder()
                    .refreshToken("expired-token")
                    .build();

            AuthToken expiredToken = AuthToken.builder()
                    .tokenId(2L)
                    .user(customerUser)
                    .tokenType(TokenType.REFRESH_TOKEN)
                    .expiresAt(LocalDateTime.now().minusHours(1))
                    .build();

            when(authTokenRepository.findByTokenAndTokenType(anyString(), any(TokenType.class)))
                    .thenReturn(Optional.of(expiredToken));

            assertThatThrownBy(() -> authService.refreshToken(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("hết hạn");

            verify(authTokenRepository).delete(expiredToken);
        }
    }

    @Nested
    @DisplayName("Forgot Password Tests")
    class ForgotPasswordTests {

        @Test
        @DisplayName("forgotPassword should generate OTP and send email when user exists and is active")
        void forgotPassword_activeUser_sendsOtp() {
            ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                    .email("customer@store.com")
                    .build();

            when(userRepository.findByEmail("customer@store.com")).thenReturn(Optional.of(customerUser));
            when(otpService.generateAndSaveOtp("customer@store.com")).thenReturn("123456");

            authService.forgotPassword(request);

            verify(otpService).generateAndSaveOtp("customer@store.com");
            verify(emailService).sendOtpEmail("customer@store.com", "Customer User", "123456");
        }

        @Test
        @DisplayName("forgotPassword should not fail and not send OTP when email does not exist (silent fail for security)")
        void forgotPassword_nonExistentEmail_silent() {
            ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                    .email("nonexistent@store.com")
                    .build();

            when(userRepository.findByEmail("nonexistent@store.com")).thenReturn(Optional.empty());

            // Should complete without exception to prevent email enumeration
            authService.forgotPassword(request);

            verify(userRepository).findByEmail("nonexistent@store.com");
        }

        @Test
        @DisplayName("forgotPassword should not send OTP when user status is BANNED (silent return for security)")
        void forgotPassword_bannedUser_silent() {
            customerUser.setStatus(UserStatus.BANNED);
            ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                    .email("customer@store.com")
                    .build();

            when(userRepository.findByEmail("customer@store.com")).thenReturn(Optional.of(customerUser));

            authService.forgotPassword(request);

            verify(userRepository).findByEmail("customer@store.com");
        }

        @Test
        @DisplayName("verifyOtp should return reset token when OTP is valid")
        void verifyOtp_validOtp_returnsResetToken() {
            VerifyOtpRequest request = VerifyOtpRequest.builder()
                    .email("customer@store.com")
                    .otp("123456")
                    .build();

            when(otpService.verifyOtpAndGenerateResetToken("customer@store.com", "123456")).thenReturn("uuid-reset-token-xyz");

            VerifyOtpResponse response = authService.verifyOtp(request);

            assertThat(response).isNotNull();
            assertThat(response.getResetToken()).isEqualTo("uuid-reset-token-xyz");
            assertThat(response.getEmail()).isEqualTo("customer@store.com");
        }

        @Test
        @DisplayName("verifyOtp should throw exception when OTP is invalid or expired")
        void verifyOtp_invalidOtp_throwsException() {
            VerifyOtpRequest request = VerifyOtpRequest.builder()
                    .email("customer@store.com")
                    .otp("999999")
                    .build();

            when(otpService.verifyOtpAndGenerateResetToken("customer@store.com", "999999"))
                    .thenThrow(new IllegalArgumentException("Mã OTP không chính xác hoặc đã hết hạn"));

            assertThatThrownBy(() -> authService.verifyOtp(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Mã OTP không chính xác hoặc đã hết hạn");
        }

        @Test
        @DisplayName("resetPassword should update password and revoke refresh tokens when reset token is valid")
        void resetPassword_validToken_success() {
            ResetPasswordRequest request = ResetPasswordRequest.builder()
                    .email("customer@store.com")
                    .resetToken("valid-reset-token")
                    .newPassword("newSecr3t!")
                    .confirmPassword("newSecr3t!")
                    .build();

            when(otpService.validateResetToken("customer@store.com", "valid-reset-token")).thenReturn(true);
            when(userRepository.findByEmail("customer@store.com")).thenReturn(Optional.of(customerUser));
            when(passwordEncoder.encode("newSecr3t!")).thenReturn("$2a$10$newHashedPassword");

            authService.resetPassword(request);

            assertThat(customerUser.getPasswordHash()).isEqualTo("$2a$10$newHashedPassword");
            verify(userRepository).save(customerUser);
            verify(authTokenRepository).deleteByUser_UserIdAndTokenType(customerUser.getUserId(), TokenType.REFRESH_TOKEN);
            verify(otpService).clearResetToken("customer@store.com", "valid-reset-token");
        }

        @Test
        @DisplayName("resetPassword should throw exception when confirm password does not match")
        void resetPassword_passwordMismatch() {
            ResetPasswordRequest request = ResetPasswordRequest.builder()
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
            ResetPasswordRequest request = ResetPasswordRequest.builder()
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

    @Nested
    @DisplayName("Social Login Tests")
    class SocialLoginTests {

        private SocialUserInfo sampleGoogleUser;
        private SocialUserInfo sampleFacebookUser;
        private GoogleLoginRequest googleRequest;
        private FacebookLoginRequest facebookRequest;
        private SocialUserInfo sampleZaloUser;
        private ZaloLoginRequest zaloRequest;

        @BeforeEach
        void initSocial() {
            sampleGoogleUser = SocialUserInfo.builder()
                    .provider(AuthProvider.GOOGLE)
                    .providerId("google-sub-12345")
                    .email("customer@store.com")
                    .fullName("Google Customer")
                    .avatarUrl("https://lh3.googleusercontent.com/avatar")
                    .emailVerified(true)
                    .build();

            sampleFacebookUser = SocialUserInfo.builder()
                    .provider(AuthProvider.FACEBOOK)
                    .providerId("fb-id-67890")
                    .email("customer@store.com")
                    .fullName("Facebook Customer")
                    .avatarUrl("https://graph.facebook.com/avatar")
                    .emailVerified(true)
                    .build();

            sampleZaloUser = SocialUserInfo.builder()
                    .provider(AuthProvider.ZALO)
                    .providerId("zalo-user-123456")
                    .email("zalo_zalo-user-123456@zalo.me")
                    .fullName("Zalo Customer")
                    .avatarUrl("https://graph.zalo.me/avatar")
                    .emailVerified(true)
                    .build();

            googleRequest = GoogleLoginRequest.builder()
                    .idToken("valid-google-id-token")
                    .build();

            facebookRequest = FacebookLoginRequest.builder()
                    .accessToken("valid-facebook-access-token")
                    .build();

            zaloRequest = ZaloLoginRequest.builder()
                    .code("valid-zalo-auth-code")
                    .codeVerifier("valid-code-verifier-string")
                    .build();
        }

        @Test
        @DisplayName("loginWithGoogle - Người dùng mới -> Tạo tài khoản ROLE_CUSTOMER và phát sự kiện")
        void loginWithGoogle_newUser_createsCustomer() {
            when(socialAuthService.verifyGoogleToken("valid-google-id-token")).thenReturn(sampleGoogleUser);
            when(userRepository.findByEmail("customer@store.com")).thenReturn(Optional.empty());
            when(roleRepository.findByRoleName("ROLE_CUSTOMER")).thenReturn(Optional.of(customerRole));
            when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encodedRandomPassword");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setUserId(99L);
                return u;
            });
            when(jwtTokenProvider.generateAccessToken(any(CustomUserDetails.class))).thenReturn("access-token-xyz");
            when(jwtTokenProvider.generateRefreshToken()).thenReturn("refresh-token-raw");

            AuthResponse response = authService.loginWithGoogle(googleRequest, null);

            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("access-token-xyz");
            assertThat(response.getRefreshToken()).isEqualTo("refresh-token-raw");
            verify(userRepository).save(any(User.class));
            verify(eventPublisher).publishEvent(any(SocialLoginPostProcessEvent.class));
        }

        @Test
        @DisplayName("loginWithGoogle - Khách hàng thật đang hoạt động -> Giữ nguyên mật khẩu cũ, liên kết tài khoản")
        void loginWithGoogle_existingActiveCustomer_preservesPassword() {
            String originalPasswordHash = customerUser.getPasswordHash();
            customerUser.setLastLoginAt(LocalDateTime.now().minusDays(1));
            customerUser.setCreatedAt(LocalDateTime.now().minusDays(30));

            when(socialAuthService.verifyGoogleToken("valid-google-id-token")).thenReturn(sampleGoogleUser);
            when(userRepository.findByEmail("customer@store.com")).thenReturn(Optional.of(customerUser));
            when(orderRepository.existsByUserUserId(customerUser.getUserId())).thenReturn(true);
            when(userRepository.save(any(User.class))).thenReturn(customerUser);
            when(jwtTokenProvider.generateAccessToken(any(CustomUserDetails.class))).thenReturn("access-token-xyz");
            when(jwtTokenProvider.generateRefreshToken()).thenReturn("refresh-token-raw");

            AuthResponse response = authService.loginWithGoogle(googleRequest, null);

            assertThat(response).isNotNull();
            // Mật khẩu cũ được bảo toàn 100%
            assertThat(customerUser.getPasswordHash()).isEqualTo(originalPasswordHash);
            assertThat(customerUser.getProviderId()).isEqualTo("google-sub-12345");
            assertThat(customerUser.getEmailVerified()).isTrue();
            verify(eventPublisher).publishEvent(any(SocialLoginPostProcessEvent.class));
        }

        @Test
        @DisplayName("loginWithGoogle - Tài khoản khả nghi Squatting -> Vô hiệu hóa mật khẩu, xóa refresh token cũ")
        void loginWithGoogle_squatterAccount_resetsPasswordAndRevokesTokens() {
            String originalPasswordHash = customerUser.getPasswordHash();
            customerUser.setLastLoginAt(null);
            customerUser.setEmailVerified(false);

            when(socialAuthService.verifyGoogleToken("valid-google-id-token")).thenReturn(sampleGoogleUser);
            when(userRepository.findByEmail("customer@store.com")).thenReturn(Optional.of(customerUser));
            when(orderRepository.existsByUserUserId(customerUser.getUserId())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$newRandomLockedPassword");
            when(userRepository.save(any(User.class))).thenReturn(customerUser);
            when(jwtTokenProvider.generateAccessToken(any(CustomUserDetails.class))).thenReturn("access-token-xyz");
            when(jwtTokenProvider.generateRefreshToken()).thenReturn("refresh-token-raw");

            AuthResponse response = authService.loginWithGoogle(googleRequest, null);

            assertThat(response).isNotNull();
            // Mật khẩu cũ bị vô hiệu hóa
            assertThat(customerUser.getPasswordHash()).isEqualTo("$2a$10$newRandomLockedPassword");
            assertThat(customerUser.getPasswordHash()).isNotEqualTo(originalPasswordHash);
            // Refresh token cũ bị xóa sạch
            verify(authTokenRepository).deleteByUser_UserIdAndTokenType(customerUser.getUserId(), TokenType.REFRESH_TOKEN);
            verify(eventPublisher).publishEvent(any(SocialLoginPostProcessEvent.class));
        }

        @Test
        @DisplayName("loginWithGoogle - Tài khoản bị khóa BANNED -> Ném ngoại lệ từ chối đăng nhập")
        void loginWithGoogle_bannedUser_throwsException() {
            customerUser.setStatus(UserStatus.BANNED);

            when(socialAuthService.verifyGoogleToken("valid-google-id-token")).thenReturn(sampleGoogleUser);
            when(userRepository.findByEmail("customer@store.com")).thenReturn(Optional.of(customerUser));

            assertThatThrownBy(() -> authService.loginWithGoogle(googleRequest, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Tài khoản của bạn đã bị khóa");
        }

        @Test
        @DisplayName("loginWithFacebook - Thành công cho người dùng mới")
        void loginWithFacebook_success() {
            when(socialAuthService.verifyFacebookToken("valid-facebook-access-token")).thenReturn(sampleFacebookUser);
            when(userRepository.findByEmail("customer@store.com")).thenReturn(Optional.empty());
            when(roleRepository.findByRoleName("ROLE_CUSTOMER")).thenReturn(Optional.of(customerRole));
            when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encodedRandomPassword");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setUserId(88L);
                return u;
            });
            when(jwtTokenProvider.generateAccessToken(any(CustomUserDetails.class))).thenReturn("access-token-fb");
            when(jwtTokenProvider.generateRefreshToken()).thenReturn("refresh-token-fb");

            AuthResponse response = authService.loginWithFacebook(facebookRequest, null);

            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("access-token-fb");
            verify(userRepository).save(any(User.class));
            verify(eventPublisher).publishEvent(any(SocialLoginPostProcessEvent.class));
        }

        @Test
        @DisplayName("loginWithZalo - Thành công cho người dùng mới với email tổng hợp zalo_{id}@zalo.me")
        void loginWithZalo_success_newUser() {
            when(socialAuthService.verifyZaloAuthCode("valid-zalo-auth-code", "valid-code-verifier-string"))
                    .thenReturn(sampleZaloUser);
            when(userRepository.findByEmail("zalo_zalo-user-123456@zalo.me")).thenReturn(Optional.empty());
            when(roleRepository.findByRoleName("ROLE_CUSTOMER")).thenReturn(Optional.of(customerRole));
            when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encodedRandomPassword");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setUserId(77L);
                return u;
            });
            when(jwtTokenProvider.generateAccessToken(any(CustomUserDetails.class))).thenReturn("access-token-zalo");
            when(jwtTokenProvider.generateRefreshToken()).thenReturn("refresh-token-zalo");

            AuthResponse response = authService.loginWithZalo(zaloRequest, null);

            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("access-token-zalo");
            assertThat(response.getUser().getEmail()).isEqualTo("zalo_zalo-user-123456@zalo.me");
            assertThat(response.getUser().getFullName()).isEqualTo("Zalo Customer");
            verify(userRepository).save(any(User.class));
            verify(eventPublisher).publishEvent(any(SocialLoginPostProcessEvent.class));
        }
    }
}
