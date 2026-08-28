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
}
