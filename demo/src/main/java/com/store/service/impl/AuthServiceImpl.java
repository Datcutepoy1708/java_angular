package com.store.service.impl;

import com.store.dto.request.LoginRequest;
import com.store.dto.request.RefreshTokenRequest;
import com.store.dto.request.RegisterRequest;
import com.store.dto.response.AuthResponse;
import com.store.dto.response.UserSummaryResponse;
import com.store.entity.auth.AuthToken;
import com.store.entity.auth.TokenType;
import com.store.entity.user.AuthProvider;
import com.store.entity.user.Role;
import com.store.entity.user.User;
import com.store.entity.user.UserStatus;
import com.store.exception.DuplicateResourceException;
import com.store.exception.ResourceNotFoundException;
import com.store.repository.AuthTokenRepository;
import com.store.repository.RoleRepository;
import com.store.repository.UserRepository;
import com.store.security.CustomUserDetails;
import com.store.security.JwtTokenProvider;
import com.store.security.LoginRateLimiter;
import com.store.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuthTokenRepository authTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final LoginRateLimiter loginRateLimiter;

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
