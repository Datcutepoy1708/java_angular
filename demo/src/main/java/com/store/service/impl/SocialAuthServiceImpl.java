package com.store.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.store.dto.response.auth.FacebookDebugTokenResponse;
import com.store.dto.response.auth.SocialUserInfo;
import com.store.entity.user.AuthProvider;
import com.store.service.SocialAuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;

@Slf4j
@Service
public class SocialAuthServiceImpl implements SocialAuthService {

    private final String googleClientId;
    private final String facebookAppId;
    private final String facebookAppSecret;
    private final boolean mockEnabled;
    private final Environment environment;
    private final RestClient restClient;

    public SocialAuthServiceImpl(
            @Value("${app.oauth2.google.client-id:}") String googleClientId,
            @Value("${app.oauth2.facebook.app-id:}") String facebookAppId,
            @Value("${app.oauth2.facebook.app-secret:}") String facebookAppSecret,
            @Value("${app.oauth2.mock-enabled:false}") boolean mockEnabled,
            Environment environment
    ) {
        this.googleClientId = googleClientId != null ? googleClientId.trim() : "";
        this.facebookAppId = facebookAppId != null ? facebookAppId.trim() : "";
        this.facebookAppSecret = facebookAppSecret != null ? facebookAppSecret.trim() : "";
        this.mockEnabled = mockEnabled;
        this.environment = environment;

        // Cấu hình RestClient với Connection Timeout = 5s, Read Timeout = 5s
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public SocialUserInfo verifyGoogleToken(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            throw new IllegalArgumentException("Google ID Token không được để trống");
        }

        // Kiểm tra Mock Mode (Chỉ được phép chạy khi thỏa mãn cả 2 lớp khóa: dev/test profile VÀ mock-enabled=true)
        if (isMockModeActive() && idToken.startsWith("mock_google_token_")) {
            log.warn("Mock Google login activated for testing purposes only!");
            String mockEmail = idToken.replace("mock_google_token_", "") + "@gmail.com";
            return SocialUserInfo.builder()
                    .provider(AuthProvider.GOOGLE)
                    .providerId("mock_google_sub_12345")
                    .email(mockEmail.toLowerCase())
                    .fullName("Mock Google User")
                    .avatarUrl("https://lh3.googleusercontent.com/a/default-user")
                    .emailVerified(true)
                    .build();
        }

        if (googleClientId.isBlank()) {
            throw new IllegalStateException("Google Client ID chưa được cấu hình trên máy chủ.");
        }

        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(), GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken googleIdToken = verifier.verify(idToken);
            if (googleIdToken == null) {
                log.warn("Google ID Token verification failed (null token returned)");
                throw new IllegalArgumentException("Google ID Token không hợp lệ hoặc đã hết hạn");
            }

            GoogleIdToken.Payload payload = googleIdToken.getPayload();

            if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
                log.warn("Google user email {} is not verified by Google", payload.getEmail());
                throw new IllegalArgumentException("Email Google chưa được xác minh. Vui lòng xác minh email với Google.");
            }

            String email = payload.getEmail();
            String name = (String) payload.get("name");
            String pictureUrl = (String) payload.get("picture");
            String sub = payload.getSubject();

            log.info("Successfully verified Google ID Token for email: {}", email);

            return SocialUserInfo.builder()
                    .provider(AuthProvider.GOOGLE)
                    .providerId(sub)
                    .email(email != null ? email.trim().toLowerCase() : "")
                    .fullName(name != null ? name.trim() : "Google User")
                    .avatarUrl(pictureUrl)
                    .emailVerified(true)
                    .build();

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Lỗi khi xác thực Google ID Token: {}", e.getMessage(), e);
            throw new IllegalArgumentException("Xác thực Google ID Token thất bại: " + e.getMessage());
        }
    }

    @Override
    public SocialUserInfo verifyFacebookToken(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("Facebook Access Token không được để trống");
        }

        // Kiểm tra Mock Mode (Chỉ được phép chạy khi thỏa mãn cả 2 lớp khóa: dev/test profile VÀ mock-enabled=true)
        if (isMockModeActive() && accessToken.startsWith("mock_fb_token_")) {
            log.warn("Mock Facebook login activated for testing purposes only!");
            String mockEmail = accessToken.replace("mock_fb_token_", "") + "@facebook.com";
            return SocialUserInfo.builder()
                    .provider(AuthProvider.FACEBOOK)
                    .providerId("mock_fb_id_67890")
                    .email(mockEmail.toLowerCase())
                    .fullName("Mock Facebook User")
                    .avatarUrl("https://graph.facebook.com/mock/picture")
                    .emailVerified(true)
                    .build();
        }

        if (facebookAppId.isBlank() || facebookAppSecret.isBlank()) {
            throw new IllegalStateException("Facebook App ID hoặc App Secret chưa được cấu hình trên máy chủ.");
        }

        try {
            // Bước 1: Dùng App Access Token gọi /debug_token để chống lỗ hổng Confused Deputy
            String appAccessToken = facebookAppId + "|" + facebookAppSecret;

            FacebookDebugTokenResponse debugResponse = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("graph.facebook.com")
                            .path("/v21.0/debug_token")
                            .queryParam("input_token", accessToken)
                            .queryParam("access_token", appAccessToken)
                            .build())
                    .retrieve()
                    .body(FacebookDebugTokenResponse.class);

            if (debugResponse == null || debugResponse.getData() == null || !debugResponse.getData().isValid()) {
                throw new IllegalArgumentException("Facebook Access Token không hợp lệ hoặc đã hết hạn");
            }

            // Chống Confused Deputy Attack: Kiểm tra token có đúng cấp cho Facebook App của chúng ta không
            if (!facebookAppId.equals(debugResponse.getData().getAppId())) {
                log.error("Confused Deputy Attack detected! Token appId: {}, Expected appId: {}",
                        debugResponse.getData().getAppId(), facebookAppId);
                throw new IllegalArgumentException("Facebook token không thuộc về ứng dụng này");
            }

            // Kiểm tra quyền email
            if (debugResponse.getData().getScopes() == null || !debugResponse.getData().getScopes().contains("email")) {
                throw new IllegalArgumentException("Ứng dụng chưa được cấp quyền truy cập Email trên tài khoản Facebook của bạn.");
            }

            // Bước 2: Gọi /v21.0/me để trích xuất email, họ tên, avatar
            JsonNode meNode = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("graph.facebook.com")
                            .path("/v21.0/me")
                            .queryParam("fields", "id,name,email,picture.type(large)")
                            .queryParam("access_token", accessToken)
                            .build())
                    .retrieve()
                    .body(JsonNode.class);

            if (meNode == null || !meNode.has("email") || meNode.get("email").asText().isBlank()) {
                log.warn("Facebook Graph API did not return an email for user id: {}",
                        meNode != null && meNode.has("id") ? meNode.get("id").asText() : "unknown");
                throw new IllegalArgumentException("Tài khoản Facebook của bạn chưa cung cấp địa chỉ Email. Vui lòng cấp quyền truy cập Email hoặc đăng ký bằng Email.");
            }

            String id = meNode.get("id").asText();
            String name = meNode.has("name") ? meNode.get("name").asText() : "Facebook User";
            String email = meNode.get("email").asText().trim().toLowerCase();

            String avatarUrl = null;
            if (meNode.has("picture") && meNode.get("picture").has("data") && meNode.get("picture").get("data").has("url")) {
                avatarUrl = meNode.get("picture").get("data").get("url").asText();
            }

            log.info("Successfully verified Facebook Access Token for email: {}", email);

            return SocialUserInfo.builder()
                    .provider(AuthProvider.FACEBOOK)
                    .providerId(id)
                    .email(email)
                    .fullName(name)
                    .avatarUrl(avatarUrl)
                    .emailVerified(true)
                    .build();

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Lỗi khi xác thực Facebook Token: {}", e.getMessage(), e);
            throw new IllegalArgumentException("Xác thực Facebook Token thất bại: " + e.getMessage());
        }
    }

    private boolean isMockModeActive() {
        if (!mockEnabled) {
            return false;
        }
        String[] activeProfiles = environment.getActiveProfiles();
        return Arrays.stream(activeProfiles)
                .anyMatch(p -> "dev".equalsIgnoreCase(p) || "test".equalsIgnoreCase(p));
    }
}
