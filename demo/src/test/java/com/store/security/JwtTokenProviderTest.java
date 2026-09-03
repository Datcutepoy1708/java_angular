package com.store.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    private static final String VALID_SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final long DEFAULT_EXPIRATION = 1800000L;

    @Test
    @DisplayName("Should initialize successfully when secret is >= 32 bytes (256 bits)")
    void init_Success_WithValidSecret() {
        JwtTokenProvider provider = new JwtTokenProvider(VALID_SECRET, DEFAULT_EXPIRATION);
        assertThat(provider).isNotNull();
        assertThat(provider.getAccessTokenExpirationMs()).isEqualTo(DEFAULT_EXPIRATION);
    }

    @Test
    @DisplayName("Should throw IllegalStateException when secret is null or empty")
    void init_Fails_WhenSecretIsNullOrEmpty() {
        assertThatThrownBy(() -> new JwtTokenProvider(null, DEFAULT_EXPIRATION))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT Secret cannot be null or empty");

        assertThatThrownBy(() -> new JwtTokenProvider("   ", DEFAULT_EXPIRATION))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT Secret cannot be null or empty");
    }

    @Test
    @DisplayName("Should throw IllegalStateException when secret is shorter than 32 bytes")
    void init_Fails_WhenSecretIsTooShort() {
        String shortSecret = "short-secret-under-32-chars!";
        assertThatThrownBy(() -> new JwtTokenProvider(shortSecret, DEFAULT_EXPIRATION))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 256 bits (32 bytes)");
    }
}
