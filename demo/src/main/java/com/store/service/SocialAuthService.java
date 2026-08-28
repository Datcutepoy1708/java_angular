package com.store.service;

import com.store.dto.response.auth.SocialUserInfo;

public interface SocialAuthService {

    /**
     * Xác thực Google ID Token cục bộ (offline cryptographic verification) qua GoogleIdTokenVerifier.
     *
     * @param idToken Google ID Token dạng JWT do GIS trả về
     * @return SocialUserInfo chứa thông tin người dùng đã chuẩn hóa
     */
    SocialUserInfo verifyGoogleToken(String idToken);

    /**
     * Xác thực Facebook User Access Token:
     * 1. Gọi /v21.0/debug_token bằng App Access Token để chống tấn công Confused Deputy.
     * 2. Gọi /v21.0/me để trích xuất email, họ tên, avatar.
     *
     * @param accessToken User Access Token do Facebook SDK trả về
     * @return SocialUserInfo chứa thông tin người dùng đã chuẩn hóa
     */
    SocialUserInfo verifyFacebookToken(String accessToken);

    /**
     * Xác thực Zalo OAuth v4 PKCE:
     * 1. Gọi POST /v4/access_token với Authorization Code và Code Verifier để lấy Access Token.
     * 2. Gọi GET /v2.0/me để lấy Zalo ID, tên hiển thị và avatar.
     *
     * @param code Authorization Code do Zalo OAuth trả về
     * @param codeVerifier Mã bí mật xác minh PKCE sinh ra từ client
     * @return SocialUserInfo chứa thông tin người dùng Zalo đã chuẩn hóa
     */
    SocialUserInfo verifyZaloAuthCode(String code, String codeVerifier);
}
