package com.store.service.impl;

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
import com.store.dto.response.UserSummaryResponse;
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
import com.store.exception.ResourceNotFoundException;
import com.store.repository.AuthTokenRepository;
import com.store.repository.OrderRepository;
import com.store.repository.RoleRepository;
import com.store.repository.UserRepository;
import com.store.security.CustomUserDetails;
import com.store.security.JwtTokenProvider;
import com.store.security.LoginRateLimiter;
import com.store.service.AuthService;
import com.store.service.EmailService;
import com.store.service.OtpService;
import com.store.service.SocialAuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuthTokenRepository authTokenRepository;
    private final OrderRepository orderRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final LoginRateLimiter loginRateLimiter;
    private final EmailService emailService;
    private final OtpService otpService;
    private final SocialAuthService socialAuthService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new customer account with email: {}", request.getEmail());

        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email " + email + " đã được đăng ký trên hệ thống");
        }

        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            if (userRepository.existsByPhone(request.getPhone().trim())) {
                throw new DuplicateResourceException("Số điện thoại " + request.getPhone() + " đã được sử dụng");
            }
        }

        Role customerRole = roleRepository.findByRoleName("ROLE_CUSTOMER")
                .orElseGet(() -> roleRepository.save(
                        Role.builder()
                                .roleName("ROLE_CUSTOMER")
                                .description("Default customer role")
                                .build()
                ));

        Set<Role> roles = new HashSet<>();
        roles.add(customerRole);

        User user = User.builder()
                .fullName(request.getFullName().trim())
                .email(email)
                .phone(request.getPhone() != null && !request.getPhone().isBlank() ? request.getPhone().trim() : null)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .status(UserStatus.ACTIVE)
                .provider(AuthProvider.LOCAL)
                .roles(roles)
                .build();

        User savedUser = userRepository.save(user);

        return buildAuthResponse(savedUser);
    }

    @Override
    @Transactional
    public AuthResponse customerLogin(LoginRequest request) {
        log.info("Customer login attempt for email: {}", request.getEmail());

        User user = authenticateAndValidateUser(request.getEmail(), request.getPassword());

        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse adminLogin(LoginRequest request) {
        log.info("Admin portal login attempt for email: {}", request.getEmail());

        User user = authenticateAndValidateUser(request.getEmail(), request.getPassword());

        // Kiểm tra nghiêm ngặt quyền Admin / Staff sau khi đã xác thực mật khẩu
        boolean hasAdminPrivilege = user.getRoles().stream()
                .anyMatch(r -> "ROLE_ADMIN".equalsIgnoreCase(r.getRoleName()) || "ROLE_STAFF".equalsIgnoreCase(r.getRoleName()));

        if (!hasAdminPrivilege) {
            log.warn("Unauthorized admin portal login attempt by customer: {}", user.getEmail());
            throw new AccessDeniedException("Truy cập bị từ chối: Tài khoản không có quyền truy cập trang quản trị");
        }

        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        log.info("Refreshing access token");

        String tokenHash = hashToken(request.getRefreshToken().trim());
        AuthToken authToken = authTokenRepository.findByTokenAndTokenType(tokenHash, TokenType.REFRESH_TOKEN)
                .orElseThrow(() -> new IllegalArgumentException("Refresh token không hợp lệ hoặc không tồn tại"));

        if (authToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            authTokenRepository.delete(authToken);
            throw new IllegalArgumentException("Refresh token đã hết hạn, vui lòng đăng nhập lại");
        }

        User user = authToken.getUser();
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException("Tài khoản đã bị khóa hoặc ngừng hoạt động");
        }

        CustomUserDetails userDetails = new CustomUserDetails(user);
        String newAccessToken = jwtTokenProvider.generateAccessToken(userDetails);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(request.getRefreshToken().trim())
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenExpirationMs() / 1000)
                .user(UserSummaryResponse.fromEntity(user))
                .build();
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            log.info("Revoking refresh token on logout");
            String tokenHash = hashToken(refreshToken.trim());
            authTokenRepository.deleteByToken(tokenHash);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UserSummaryResponse getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new AccessDeniedException("Người dùng chưa đăng nhập");
        }

        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin tài khoản: " + email));

        return UserSummaryResponse.fromEntity(user);
    }

    private User authenticateAndValidateUser(String email, String password) {
        String normalizedEmail = email.trim().toLowerCase();

        // 1. Kiểm tra Rate Limiting chống Brute-force qua Redis
        loginRateLimiter.checkRateLimit(normalizedEmail, null);

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> {
                    loginRateLimiter.recordFailedAttempt(normalizedEmail, null);
                    return new IllegalArgumentException("Email hoặc mật khẩu không chính xác");
                });

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            loginRateLimiter.recordFailedAttempt(normalizedEmail, null);
            throw new IllegalArgumentException("Email hoặc mật khẩu không chính xác");
        }

        if (user.getStatus() == UserStatus.BANNED) {
            throw new IllegalArgumentException("Tài khoản của bạn đã bị khóa");
        }

        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new IllegalArgumentException("Tài khoản chưa được kích hoạt");
        }

        // Đăng nhập thành công -> Reset số lần thử sai
        loginRateLimiter.resetAttempts(normalizedEmail, null);

        user.setLastLoginAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        CustomUserDetails userDetails = new CustomUserDetails(user);
        String accessToken = jwtTokenProvider.generateAccessToken(userDetails);
        String rawRefreshToken = jwtTokenProvider.generateRefreshToken();
        String tokenHash = hashToken(rawRefreshToken);

        // Lưu HASH của refresh token vào DB (SHA-256, thời hạn 7 ngày)
        AuthToken tokenEntity = AuthToken.builder()
                .user(user)
                .token(tokenHash)
                .tokenType(TokenType.REFRESH_TOKEN)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
        authTokenRepository.save(tokenEntity);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenExpirationMs() / 1000)
                .user(UserSummaryResponse.fromEntity(user))
                .build();
    }

    @Override
    public void forgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        log.info("Processing forgot password request for email: {}", email);

        // Security best practice: Do not leak account existence to prevent user enumeration
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            log.warn("Forgot password requested for non-existent email: {}", email);
            return;
        }

        User user = userOpt.get();
        if (user.getStatus() != UserStatus.ACTIVE) {
            log.warn("Forgot password requested for non-active user: {} (status: {})", email, user.getStatus());
            return;
        }

        // Generate OTP and store in Redis with rate limits
        String otp = otpService.generateAndSaveOtp(email);

        // Send OTP email asynchronously
        emailService.sendOtpEmail(user.getEmail(), user.getFullName(), otp);
    }

    @Override
    public VerifyOtpResponse verifyOtp(VerifyOtpRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        log.info("Verifying OTP for email: {}", email);

        String resetToken = otpService.verifyOtpAndGenerateResetToken(email, request.getOtp());

        return VerifyOtpResponse.builder()
                .resetToken(resetToken)
                .email(email)
                .expiresInSeconds(600)
                .build();
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        log.info("Resetting password for email: {}", email);

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Mật khẩu xác nhận không khớp");
        }

        // Validate reset token from Redis
        boolean isValidToken = otpService.validateResetToken(email, request.getResetToken());
        if (!isValidToken) {
            throw new IllegalArgumentException("Phiên đặt lại mật khẩu không hợp lệ hoặc đã hết hạn. Vui lòng thử lại từ đầu.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với email: " + email));

        // Update password with BCrypt
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Revoke all existing refresh tokens for security
        authTokenRepository.deleteByUser_UserIdAndTokenType(user.getUserId(), TokenType.REFRESH_TOKEN);

        // Clear one-time reset token
        otpService.clearResetToken(email, request.getResetToken());

        log.info("Successfully reset password and revoked old sessions for user: {}", email);
    }

    @Override
    @Transactional
    public AuthResponse loginWithGoogle(GoogleLoginRequest request, HttpServletRequest httpRequest) {
        String clientIp = extractClientIp(httpRequest);
        loginRateLimiter.checkRateLimit(null, clientIp);

        SocialUserInfo socialUser = socialAuthService.verifyGoogleToken(request.getIdToken());
        return processSocialLogin(socialUser, httpRequest, clientIp);
    }

    @Override
    @Transactional
    public AuthResponse loginWithFacebook(FacebookLoginRequest request, HttpServletRequest httpRequest) {
        String clientIp = extractClientIp(httpRequest);
        loginRateLimiter.checkRateLimit(null, clientIp);

        SocialUserInfo socialUser = socialAuthService.verifyFacebookToken(request.getAccessToken());
        return processSocialLogin(socialUser, httpRequest, clientIp);
    }

    @Override
    @Transactional
    public AuthResponse loginWithZalo(ZaloLoginRequest request, HttpServletRequest httpRequest) {
        String clientIp = extractClientIp(httpRequest);
        loginRateLimiter.checkRateLimit(null, clientIp);

        SocialUserInfo socialUser = socialAuthService.verifyZaloAuthCode(request.getCode(), request.getCodeVerifier());
        return processSocialLogin(socialUser, httpRequest, clientIp);
    }

    private AuthResponse processSocialLogin(SocialUserInfo socialUser, HttpServletRequest httpRequest, String clientIp) {
        String email = socialUser.getEmail().trim().toLowerCase();
        String userAgent = httpRequest != null ? httpRequest.getHeader("User-Agent") : "Unknown";

        Optional<User> existingUserOpt = userRepository.findByEmail(email);

        User user;
        boolean preHijackSuspected = false;
        boolean isNewUser = false;

        if (existingUserOpt.isPresent()) {
            user = existingUserOpt.get();

            if (user.getStatus() == UserStatus.BANNED) {
                log.warn("Banned user attempted social login: {}", email);
                throw new IllegalArgumentException("Tài khoản của bạn đã bị khóa. Vui lòng liên hệ quản trị viên.");
            }

            // Kiểm tra phân loại 3 lớp: Khách hàng thật vs Squatter
            boolean hasOrders = orderRepository.existsByUserUserId(user.getUserId());
            boolean hasActiveLoginHistory = user.getLastLoginAt() != null
                    && user.getCreatedAt() != null
                    && user.getLastLoginAt().isAfter(user.getCreatedAt().plusMinutes(5));
            boolean isEmailAlreadyVerified = Boolean.TRUE.equals(user.getEmailVerified());

            boolean isLikelyRealCustomer = isEmailAlreadyVerified || hasOrders || hasActiveLoginHistory;

            if (isLikelyRealCustomer) {
                // Nhóm 1: Khách hàng thật đang hoạt động -> BẢO TOÀN MẬT KHẨU CŨ 100%
                user.setProviderId(socialUser.getProviderId());
                user.setEmailVerified(true);
                if (user.getAvatarUrl() == null || user.getAvatarUrl().isBlank()) {
                    user.setAvatarUrl(socialUser.getAvatarUrl());
                }
                user.setLastLoginAt(LocalDateTime.now());
                user = userRepository.save(user);

                log.info("Linked social account for existing real customer: email={}, provider={}", email, socialUser.getProvider());
            } else {
                // Nhóm 2: Khả nghi Squatting / Pre-hijack (chưa có đơn hàng, chưa từng login sau khi tạo, chưa verify email)
                preHijackSuspected = true;
                log.warn("Suspected squatter account detected for email: {}. Resetting password and revoking old sessions.", email);

                // 1. Vô hiệu hóa mật khẩu cũ của kẻ tạo
                user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));

                // 2. Xóa refresh token cũ trong CSDL
                authTokenRepository.deleteByUser_UserIdAndTokenType(user.getUserId(), TokenType.REFRESH_TOKEN);

                // 3. Cập nhật quyền sở hữu cho người dùng xã hội
                user.setProvider(socialUser.getProvider());
                user.setProviderId(socialUser.getProviderId());
                user.setEmailVerified(true);
                if (user.getAvatarUrl() == null || user.getAvatarUrl().isBlank()) {
                    user.setAvatarUrl(socialUser.getAvatarUrl());
                }
                user.setLastLoginAt(LocalDateTime.now());
                user = userRepository.save(user);
            }
        } else {
            // Người dùng hoàn toàn mới -> Tạo mới
            isNewUser = true;
            Role customerRole = roleRepository.findByRoleName("ROLE_CUSTOMER")
                    .orElseGet(() -> roleRepository.save(
                            Role.builder()
                                    .roleName("ROLE_CUSTOMER")
                                    .description("Default customer role")
                                    .build()
                    ));

            Set<Role> roles = new HashSet<>();
            roles.add(customerRole);

            user = User.builder()
                    .fullName(socialUser.getFullName() != null && !socialUser.getFullName().isBlank() ? socialUser.getFullName().trim() : "Khách hàng")
                    .email(email)
                    .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .status(UserStatus.ACTIVE)
                    .provider(socialUser.getProvider())
                    .providerId(socialUser.getProviderId())
                    .avatarUrl(socialUser.getAvatarUrl())
                    .emailVerified(true)
                    .lastLoginAt(LocalDateTime.now())
                    .roles(roles)
                    .build();

            try {
                user = userRepository.save(user);
                log.info("Created new customer account from social login: email={}, provider={}", email, socialUser.getProvider());
            } catch (DataIntegrityViolationException dive) {
                // Chống race condition: nếu có request song song vừa tạo user cùng email
                log.warn("Race condition detected when creating social user for email {}. Re-fetching user.", email);
                user = userRepository.findByEmail(email)
                        .orElseThrow(() -> new IllegalStateException("Lỗi xung đột dữ liệu khi đăng ký tài khoản"));
            }
        }

        // Bắn sự kiện bất đồng bộ sau khi commit thành công (gửi email, ghi Redis, audit log)
        eventPublisher.publishEvent(SocialLoginPostProcessEvent.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .provider(socialUser.getProvider().name())
                .providerId(socialUser.getProviderId())
                .preHijackSuspected(preHijackSuspected)
                .isNewUser(isNewUser)
                .clientIp(clientIp)
                .userAgent(userAgent)
                .build());

        return buildAuthResponse(user);
    }

    private String extractClientIp(HttpServletRequest request) {
        if (request == null) return "127.0.0.1";
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }
        return request.getRemoteAddr();
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
