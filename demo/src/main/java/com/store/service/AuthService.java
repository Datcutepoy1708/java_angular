package com.store.service;

import com.store.dto.request.LoginRequest;
import com.store.dto.request.RefreshTokenRequest;
import com.store.dto.request.RegisterRequest;
import com.store.dto.request.auth.ForgotPasswordRequest;
import com.store.dto.request.auth.ResetPasswordRequest;
import com.store.dto.request.auth.VerifyOtpRequest;
import com.store.dto.response.AuthResponse;
import com.store.dto.response.UserSummaryResponse;
import com.store.dto.response.auth.VerifyOtpResponse;

import com.store.dto.request.auth.FacebookLoginRequest;
import com.store.dto.request.auth.GoogleLoginRequest;
import com.store.dto.request.auth.ZaloLoginRequest;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse customerLogin(LoginRequest request);

    AuthResponse adminLogin(LoginRequest request);

    AuthResponse refreshToken(RefreshTokenRequest request);

    void logout(String refreshToken);

    UserSummaryResponse getCurrentUser();

    void forgotPassword(ForgotPasswordRequest request);

    VerifyOtpResponse verifyOtp(VerifyOtpRequest request);

    void resetPassword(ResetPasswordRequest request);

    AuthResponse loginWithGoogle(GoogleLoginRequest request, HttpServletRequest httpRequest);

    AuthResponse loginWithFacebook(FacebookLoginRequest request, HttpServletRequest httpRequest);

    AuthResponse loginWithZalo(ZaloLoginRequest request, HttpServletRequest httpRequest);
}
