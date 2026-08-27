package com.store.service;

public interface OtpService {

    String generateAndSaveOtp(String email);

    String verifyOtpAndGenerateResetToken(String email, String otp);

    boolean validateResetToken(String email, String resetToken);

    void clearResetToken(String email, String resetToken);
}
