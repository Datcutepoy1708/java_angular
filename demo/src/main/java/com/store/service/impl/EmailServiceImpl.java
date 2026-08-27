package com.store.service.impl;

import com.store.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from-address:no-reply@complexus.vn}")
    private String fromAddress;

    @Value("${app.mail.from-name:Complexus Shop}")
    private String fromName;

    @Async
    @Override
    public void sendOtpEmail(String toEmail, String recipientName, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress, fromName);
            helper.setTo(toEmail);
            helper.setSubject("[COMPLEXUS] Mã xác thực đặt lại mật khẩu của bạn: " + otp);

            String greeting = (recipientName != null && !recipientName.isBlank())
                    ? "Xin chào " + recipientName + ","
                    : "Xin chào quý khách,";

            String htmlContent = buildOtpEmailHtml(greeting, otp);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Successfully sent OTP email to: {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send OTP email to {}: {}", toEmail, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error sending email to {}: {}", toEmail, e.getMessage(), e);
        }
    }

    private String buildOtpEmailHtml(String greeting, String otp) {
        return "<!DOCTYPE html>"
                + "<html>"
                + "<head><meta charset='UTF-8'></head>"
                + "<body style='margin:0;padding:0;background-color:#f8fafc;font-family:Arial,sans-serif;color:#1e293b;'>"
                + "<table width='100%' cellpadding='0' cellspacing='0' style='background-color:#f8fafc;padding:40px 0;'>"
                + "  <tr>"
                + "    <td align='center'>"
                + "      <table width='540' cellpadding='0' cellspacing='0' style='background-color:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 4px 6px -1px rgba(0,0,0,0.05);border:1px solid #e2e8f0;'>"
                + "        <!-- Header -->"
                + "        <tr>"
                + "          <td style='background-color:#0f172a;padding:28px 32px;text-align:center;'>"
                + "            <h1 style='margin:0;color:#ffffff;font-size:24px;font-weight:800;letter-spacing:1px;'>COMPLEXUS</h1>"
                + "            <p style='margin:4px 0 0 0;color:#94a3b8;font-size:12px;text-transform:uppercase;letter-spacing:1.5px;'>Computer & PC Components</p>"
                + "          </td>"
                + "        </tr>"
                + "        <!-- Body -->"
                + "        <tr>"
                + "          <td style='padding:32px;'>"
                + "            <h2 style='margin:0 0 16px 0;font-size:18px;color:#0f172a;font-weight:700;'>Yêu Cầu Đặt Lại Mật Khẩu</h2>"
                + "            <p style='margin:0 0 12px 0;font-size:14px;line-height:1.5;color:#475569;'>" + greeting + "</p>"
                + "            <p style='margin:0 0 24px 0;font-size:14px;line-height:1.5;color:#475569;'>Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản liên kết với địa chỉ email này. Vui lòng sử dụng mã xác thực OTP bên dưới để tiếp tục:</p>"
                + "            <!-- OTP Box -->"
                + "            <div style='background-color:#eff6ff;border:1.5px dashed #3b82f6;border-radius:10px;padding:20px;text-align:center;margin:0 0 24px 0;'>"
                + "              <div style='font-size:12px;color:#1e40af;font-weight:600;margin-bottom:6px;text-transform:uppercase;letter-spacing:1px;'>Mã xác thực OTP (6 chữ số)</div>"
                + "              <div style='font-size:32px;font-weight:800;letter-spacing:8px;color:#1d4ed8;font-family:monospace;'>" + otp + "</div>"
                + "              <div style='font-size:12px;color:#64748b;margin-top:8px;'>Có hiệu lực trong vòng <strong>5 phút</strong></div>"
                + "            </div>"
                + "            <p style='margin:0 0 12px 0;font-size:13px;line-height:1.5;color:#64748b;'><strong>Lưu ý bảo mật:</strong> Tuyệt đối không chia sẻ mã này cho bất kỳ ai, kể cả nhân viên Complexus.</p>"
                + "            <p style='margin:0;font-size:13px;line-height:1.5;color:#64748b;'>Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email hoặc đổi mật khẩu ngay để đảm bảo an toàn.</p>"
                + "          </td>"
                + "        </tr>"
                + "        <!-- Footer -->"
                + "        <tr>"
                + "          <td style='background-color:#f1f5f9;padding:20px 32px;text-align:center;border-top:1px solid #e2e8f0;font-size:12px;color:#64748b;'>"
                + "            <p style='margin:0;'>© 2026 COMPLEXUS Computer Store. All rights reserved.</p>"
                + "            <p style='margin:4px 0 0 0;'>Hotline hỗ trợ: <strong>1800 6868</strong> (8:00 - 21:30)</p>"
                + "          </td>"
                + "        </tr>"
                + "      </table>"
                + "    </td>"
                + "  </tr>"
                + "</table>"
                + "</body>"
                + "</html>";
    }
}
