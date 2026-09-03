package com.store.util;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.regex.Pattern;

@Component
public class PaymentSecurityUtil {

    // Prefix for computer store payment reference
    public static final String REFERENCE_PREFIX = "CS";
    // Custom alphabet omitting 0, 1, O, I to avoid visual confusion
    public static final String CROCKFORD_ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    public static final int RANDOM_PART_LENGTH = 10;
    public static final String REFERENCE_REGEX = "(?<![A-Z0-9])CS[23456789ABCDEFGHJKLMNPQRSTUVWXYZ]{10}(?![A-Z0-9])";
    public static final Pattern REFERENCE_PATTERN = Pattern.compile(REFERENCE_REGEX);

    private final SecureRandom secureRandom = new SecureRandom();

    public String generatePaymentReference() {
        StringBuilder sb = new StringBuilder(REFERENCE_PREFIX);
        for (int i = 0; i < RANDOM_PART_LENGTH; i++) {
            int index = secureRandom.nextInt(CROCKFORD_ALPHABET.length());
            sb.append(CROCKFORD_ALPHABET.charAt(index));
        }
        return sb.toString();
    }

    public String generateRawPollingToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    public String sha256Hex(String input) {
        if (input == null) return null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    public boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(aBytes, bBytes);
    }

    public String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.isBlank()) {
            return "******";
        }
        String trimmed = accountNumber.trim();
        if (trimmed.length() <= 4) {
            return "****";
        }
        return "******" + trimmed.substring(trimmed.length() - 4);
    }
}
