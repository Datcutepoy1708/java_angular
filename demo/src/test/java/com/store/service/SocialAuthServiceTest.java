package com.store.service;

import com.store.dto.response.auth.SocialUserInfo;
import com.store.entity.user.AuthProvider;
import com.store.service.impl.SocialAuthServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SocialAuthServiceTest {

    @Mock
    private Environment environment;

    private SocialAuthServiceImpl createService(String googleClientId, String fbAppId, String fbSecret, String zaloAppId, String zaloSecret, boolean mockEnabled) {
        return new SocialAuthServiceImpl(googleClientId, fbAppId, fbSecret, zaloAppId, zaloSecret, mockEnabled, environment);
    }

    @Nested
    @DisplayName("Google Token Verification Tests")
    class GoogleTokenTests {

        @Test
        @DisplayName("verifyGoogleToken - Ném ngoại lệ khi token rỗng")
        void verifyGoogleToken_blankToken_throwsException() {
            SocialAuthServiceImpl service = createService("google-client-id", "fb-app-id", "fb-secret", "zalo-app-id", "zalo-secret", false);

            assertThatThrownBy(() -> service.verifyGoogleToken(""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Google ID Token không được để trống");
        }

        @Test
        @DisplayName("verifyGoogleToken - Ném ngoại lệ khi chưa cấu hình Google Client ID")
        void verifyGoogleToken_missingClientId_throwsException() {
            SocialAuthServiceImpl service = createService("", "fb-app-id", "fb-secret", "zalo-app-id", "zalo-secret", false);

            assertThatThrownBy(() -> service.verifyGoogleToken("some.jwt.token"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Google Client ID chưa được cấu hình");
        }

        @Test
        @DisplayName("verifyGoogleToken - Chế độ Mock an toàn khi bật mock-enabled và profile dev")
        void verifyGoogleToken_mockModeActive_success() {
            when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});

            SocialAuthServiceImpl service = createService("", "fb-app-id", "fb-secret", "zalo-app-id", "zalo-secret", true);

            SocialUserInfo result = service.verifyGoogleToken("mock_google_token_testuser");

            assertThat(result).isNotNull();
            assertThat(result.getEmail()).isEqualTo("testuser@gmail.com");
            assertThat(result.getProvider()).isEqualTo(AuthProvider.GOOGLE);
            assertThat(result.isEmailVerified()).isTrue();
        }

        @Test
        @DisplayName("verifyGoogleToken - Token giả lập bị từ chối khi mock-enabled = false")
        void verifyGoogleToken_mockModeDisabled_rejectsMockToken() {
            SocialAuthServiceImpl service = createService("", "fb-app-id", "fb-secret", "zalo-app-id", "zalo-secret", false);

            assertThatThrownBy(() -> service.verifyGoogleToken("mock_google_token_testuser"))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("Facebook Token Verification Tests")
    class FacebookTokenTests {

        @Test
        @DisplayName("verifyFacebookToken - Ném ngoại lệ khi token rỗng")
        void verifyFacebookToken_blankToken_throwsException() {
            SocialAuthServiceImpl service = createService("google-client-id", "fb-app-id", "fb-secret", "zalo-app-id", "zalo-secret", false);

            assertThatThrownBy(() -> service.verifyFacebookToken(""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Facebook Access Token không được để trống");
        }

        @Test
        @DisplayName("verifyFacebookToken - Ném ngoại lệ khi chưa cấu hình Facebook App ID")
        void verifyFacebookToken_missingAppId_throwsException() {
            SocialAuthServiceImpl service = createService("google-client-id", "", "", "zalo-app-id", "zalo-secret", false);

            assertThatThrownBy(() -> service.verifyFacebookToken("some_fb_token"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Facebook App ID hoặc App Secret chưa được cấu hình");
        }

        @Test
        @DisplayName("verifyFacebookToken - Chế độ Mock an toàn khi bật mock-enabled và profile test")
        void verifyFacebookToken_mockModeActive_success() {
            when(environment.getActiveProfiles()).thenReturn(new String[]{"test"});

            SocialAuthServiceImpl service = createService("", "fb-app-id", "fb-secret", "zalo-app-id", "zalo-secret", true);

            SocialUserInfo result = service.verifyFacebookToken("mock_fb_token_testuser");

            assertThat(result).isNotNull();
            assertThat(result.getEmail()).isEqualTo("testuser@facebook.com");
            assertThat(result.getProvider()).isEqualTo(AuthProvider.FACEBOOK);
            assertThat(result.isEmailVerified()).isTrue();
        }
    }

    @Nested
    @DisplayName("Zalo Auth Code Verification Tests")
    class ZaloTokenTests {

        @Test
        @DisplayName("verifyZaloAuthCode - Ném ngoại lệ khi code rỗng")
        void verifyZaloAuthCode_blankCode_throwsException() {
            SocialAuthServiceImpl service = createService("google-client-id", "fb-app-id", "fb-secret", "zalo-app-id", "zalo-secret", false);

            assertThatThrownBy(() -> service.verifyZaloAuthCode("", "verifier123"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Zalo Authorization Code không được để trống");
        }

        @Test
        @DisplayName("verifyZaloAuthCode - Ném ngoại lệ khi codeVerifier rỗng")
        void verifyZaloAuthCode_blankCodeVerifier_throwsException() {
            SocialAuthServiceImpl service = createService("google-client-id", "fb-app-id", "fb-secret", "zalo-app-id", "zalo-secret", false);

            assertThatThrownBy(() -> service.verifyZaloAuthCode("code123", ""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Zalo Code Verifier không được để trống");
        }

        @Test
        @DisplayName("verifyZaloAuthCode - Ném ngoại lệ khi chưa cấu hình Zalo App ID hoặc Secret")
        void verifyZaloAuthCode_missingConfig_throwsException() {
            SocialAuthServiceImpl service = createService("google-client-id", "fb-app-id", "fb-secret", "", "", false);

            assertThatThrownBy(() -> service.verifyZaloAuthCode("code123", "verifier123"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Zalo App ID hoặc Secret Key chưa được cấu hình");
        }

        @Test
        @DisplayName("verifyZaloAuthCode - Chế độ Mock an toàn khi bật mock-enabled và profile dev")
        void verifyZaloAuthCode_mockModeActive_success() {
            when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});

            SocialAuthServiceImpl service = createService("", "", "", "zalo-app-id", "zalo-secret", true);

            SocialUserInfo result = service.verifyZaloAuthCode("mock_zalo_code_user999", "verifier123");

            assertThat(result).isNotNull();
            assertThat(result.getProviderId()).isEqualTo("mock_zalo_id_user999");
            assertThat(result.getEmail()).isEqualTo("zalo_mock_user999@zalo.me");
            assertThat(result.getFullName()).isEqualTo("Mock Zalo User user999");
            assertThat(result.getProvider()).isEqualTo(AuthProvider.ZALO);
            assertThat(result.isEmailVerified()).isTrue();
        }
    }
}
