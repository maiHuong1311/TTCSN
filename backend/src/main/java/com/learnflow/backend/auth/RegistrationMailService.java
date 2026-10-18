package com.learnflow.backend.auth;

import com.learnflow.backend.common.web.ApiException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class RegistrationMailService {

    private final ObjectProvider<JavaMailSender> sender;
    private final String from;
    private final BrevoMailService brevo;

    public RegistrationMailService(
        ObjectProvider<JavaMailSender> sender,
        @Value("${application.mail.from:}") String from,
        BrevoMailService brevo
    ) {
        this.sender = sender;
        this.from = from;
        this.brevo = brevo;
    }

    public void requireConfigured() {
        if (brevo.configured()) return;
        JavaMailSender mail = sender.getIfAvailable();
        if (mail == null || from.isBlank()) throw new ApiException(
            HttpStatus.SERVICE_UNAVAILABLE,
            "Chưa cấu hình dịch vụ gửi email. Vui lòng liên hệ quản trị viên."
        );
    }

    public void sendCode(String email, String code) {
        requireConfigured();
        JavaMailSender mail = sender.getIfAvailable();
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject("FORME — Mã xác minh đăng ký tài khoản");
        message.setText(
            "Mã xác minh của bạn là: " +
                code +
                "\nMã có hiệu lực trong 10 phút, chỉ dùng một lần.\nKhông chia sẻ mã với người khác. Nếu bạn không đăng ký, hãy bỏ qua email này."
        );
        if (brevo.configured()) {
            brevo.send(email, message.getSubject(), message.getText());
            return;
        }
        try {
            mail.send(message);
        } catch (org.springframework.mail.MailException ex) {
            throw new ApiException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Không thể gửi email xác minh. Vui lòng thử lại sau."
            );
        }
    }

    public void sendResetCode(String email, String code) {
        requireConfigured();
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject("FORME — Đặt lại mật khẩu");
        message.setText(
            "Mã OTP đặt lại mật khẩu FORME của bạn là: " +
                code +
                "\nMã có hiệu lực 10 phút, chỉ dùng một lần. Không chia sẻ mã này.\nNếu bạn không yêu cầu, hãy bỏ qua email này. Mật khẩu hiện tại vẫn được giữ nguyên."
        );
        if (brevo.configured()) {
            brevo.send(email, message.getSubject(), message.getText());
            return;
        }
        try {
            sender.getIfAvailable().send(message);
        } catch (org.springframework.mail.MailException ex) {
            throw new ApiException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Không thể gửi email đặt lại mật khẩu. Vui lòng thử lại sau."
            );
        }
    }
}
