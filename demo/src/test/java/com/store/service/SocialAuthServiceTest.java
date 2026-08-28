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

    @Nested
    @DisplayName("Google Token Verification Tests")
    class GoogleTokenTests {

        @Test
        @DisplayName("verifyGoogleToken - Ném ngoại lệ khi token rỗng")
        void verifyGoogleToken_blankToken_throwsException() {
            SocialAuthServiceImpl service = new SocialAuthServiceImpl("google-client-id", "fb-app-id", "fb-secret", false, environment);

            assertThatThrownBy(() -> service.verifyGoogleToken(""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Google ID Token không được để trống");
        }

        @Test
        @DisplayName("verifyGoogleToken - Ném ngoại lệ khi chưa cấu hình Google Client ID")
        void verifyGoogleToken_missingClientId_throwsException() {
            SocialAuthServiceImpl service = new SocialAuthServiceImpl("", "fb-app-id", "fb-secret", false, environment);

            assertThatThrownBy(() -> service.verifyGoogleToken("some.jwt.token"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Google Client ID chưa được cấu hình");
        }

        @Test
        @DisplayName("verifyGoogleToken - Chế độ Mock an toàn khi bật mock-enabled và profile dev")
        void verifyGoogleToken_mockModeActive_success() {
            when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});

            SocialAuthServiceImpl service = new SocialAuthServiceImpl("", "fb-app-id", "fb-secret", true, environment);

            SocialUserInfo result = service.verifyGoogleToken("mock_google_token_testuser");

            assertThat(result).isNotNull();
            assertThat(result.getEmail()).isEqualTo("testuser@gmail.com");
            assertThat(result.getProvider()).isEqualTo(AuthProvider.GOOGLE);
            assertThat(result.isEmailVerified()).isTrue();
        }

        @Test
        @DisplayName("verifyGoogleToken - Token giả lập bị từ chối khi mock-enabled = false")
        void verifyGoogleToken_mockModeDisabled_rejectsMockToken() {
            SocialAuthServiceImpl service = new SocialAuthServiceImpl("", "fb-app-id", "fb-secret", false, environment);

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
            SocialAuthServiceImpl service = new SocialAuthServiceImpl("google-client-id", "fb-app-id", "fb-secret", false, environment);

            assertThatThrownBy(() -> service.verifyFacebookToken(""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Facebook Access Token không được để trống");
        }

        @Test
        @DisplayName("verifyFacebookToken - Ném ngoại lệ khi chưa cấu hình Facebook App ID")
        void verifyFacebookToken_missingAppId_throwsException() {
            SocialAuthServiceImpl service = new SocialAuthServiceImpl("google-client-id", "", "", false, environment);

            assertThatThrownBy(() -> service.verifyFacebookToken("some_fb_token"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Facebook App ID hoặc App Secret chưa được cấu hình");
        }

        @Test
        @DisplayName("verifyFacebookToken - Chế độ Mock an toàn khi bật mock-enabled và profile test")
        void verifyFacebookToken_mockModeActive_success() {
            when(environment.getActiveProfiles()).thenReturn(new String[]{"test"});

            SocialAuthServiceImpl service = new SocialAuthServiceImpl("", "fb-app-id", "fb-secret", true, environment);

            SocialUserInfo result = service.verifyFacebookToken("mock_fb_token_testuser");

            assertThat(result).isNotNull();
            assertThat(result.getEmail()).isEqualTo("testuser@facebook.com");
            assertThat(result.getProvider()).isEqualTo(AuthProvider.FACEBOOK);
            assertThat(result.isEmailVerified()).isTrue();
        }
    }
}
