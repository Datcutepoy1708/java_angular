package com.store.service;

public interface EmailService {
    void sendOtpEmail(String toEmail, String recipientName, String otp);
}
