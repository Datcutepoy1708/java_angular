package com.store.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentSecurityUtilTest {

    private PaymentSecurityUtil securityUtil;

    @BeforeEach
    void setUp() {
        securityUtil = new PaymentSecurityUtil();
    }

    @Test
    @DisplayName("generatePaymentReference produces CS prefix + 10-char Crockford Base32 string (total 12 chars)")
    void testGeneratePaymentReference_LengthAndAlphabet() {
        for (int i = 0; i < 50; i++) {
            String ref = securityUtil.generatePaymentReference();
            assertThat(ref).hasSize(12);
            assertThat(ref).startsWith("CS");
            String randomPart = ref.substring(2);
            assertThat(randomPart).doesNotContain("0", "1", "O", "I", "o", "i");
            assertThat(ref).matches("^CS[23456789ABCDEFGHJKLMNPQRSTUVWXYZ]{10}$");
        }
    }

    @Test
    @DisplayName("REFERENCE_PATTERN matches CS... reference within complex bank transfer description")
    void testReferencePattern_Extraction() {
        String bankContent = "FT2609040001 chuyen tien mua hang CS23456789AB MBVCB.12345678";
        Matcher matcher = PaymentSecurityUtil.REFERENCE_PATTERN.matcher(bankContent);
        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group()).isEqualTo("CS23456789AB");

        // Should not match if embedded in longer alphanumeric words
        String embedded = "PREFIXCS23456789ABSUFFIX";
        Matcher matcher2 = PaymentSecurityUtil.REFERENCE_PATTERN.matcher(embedded);
        assertThat(matcher2.find()).isFalse();
    }

    @Test
    @DisplayName("maskAccountNumber masks all but last 4 digits")
    void testMaskAccountNumber() {
        assertThat(securityUtil.maskAccountNumber("090123456789")).isEqualTo("******6789");
        assertThat(securityUtil.maskAccountNumber("1234")).isEqualTo("****");
        assertThat(securityUtil.maskAccountNumber("")).isEqualTo("******");
        assertThat(securityUtil.maskAccountNumber(null)).isEqualTo("******");
    }

    @Test
    @DisplayName("generateRawPollingToken produces 64-char hex string (32 bytes)")
    void testGenerateRawPollingToken() {
        String token = securityUtil.generateRawPollingToken();
        assertThat(token).hasSize(64);
        assertThat(token).matches("^[0-9a-f]{64}$");
    }

    @Test
    @DisplayName("sha256Hex produces deterministic lowercase 64-char hex hash")
    void testSha256Hex() {
        String input = "test-token-12345";
        String hash1 = securityUtil.sha256Hex(input);
        String hash2 = securityUtil.sha256Hex(input);

        assertThat(hash1).isNotNull().hasSize(64).isEqualTo(hash2);
        assertThat(hash1).matches("^[0-9a-f]{64}$");
    }

    @Test
    @DisplayName("constantTimeEquals correctly validates equality")
    void testConstantTimeEquals() {
        assertThat(securityUtil.constantTimeEquals("my-secret-key", "my-secret-key")).isTrue();
        assertThat(securityUtil.constantTimeEquals("my-secret-key", "wrong-secret-key")).isFalse();
        assertThat(securityUtil.constantTimeEquals(null, "key")).isFalse();
        assertThat(securityUtil.constantTimeEquals("key", null)).isFalse();
    }
}
