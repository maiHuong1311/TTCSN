package com.learnflow.backend.auth;

import com.learnflow.backend.auth.dto.*;
import com.learnflow.backend.common.web.ApiException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PasswordResetService {

    private static final AuthMessageResponse SENT = new AuthMessageResponse(
        "Nếu email thuộc tài khoản đang hoạt động, bạn sẽ nhận được mã OTP đặt lại mật khẩu. Vui lòng kiểm tra cả thư rác."
    );
    private final AuthRepository users;
    private final PasswordResetOtpStore store;
    private final RegistrationMailService mail;
    private final PasswordEncoder encoder;
    private final byte[] secret;
    private final SecureRandom random = new SecureRandom();

    public PasswordResetService(
        AuthRepository users,
        PasswordResetOtpStore store,
        RegistrationMailService mail,
        PasswordEncoder encoder,
        @Value("${application.jwt.secret}") String secret
    ) {
        this.users = users;
        this.store = store;
        this.mail = mail;
        this.encoder = encoder;
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    public AuthMessageResponse forgot(ForgotPasswordRequest request) {
        mail.requireConfigured();
        String email = normalize(request.email());
        var found = users.findByEmailIgnoreCase(email).filter(u -> "Active".equals(u.getStatus()));
        String challenge = UUID.randomUUID().toString();
        String otp;
        int issued;
        do {
            otp = String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000));
            issued = store.issue(
                email,
                codeHash(email, otp),
                found.map(u -> u.getId()).orElse(0L),
                found.map(u -> u.getAuthVersion()).orElse(0L),
                challenge
            );
        } while (issued == 2);
        if (issued == 0) return SENT;
        if (found.isEmpty()) {
            store.cancel(email, challenge);
            return SENT;
        }
        try {
            mail.sendResetCode(email, otp);
        } catch (ApiException ex) {
            store.cancel(email, challenge);
            return SENT;
        }
        store.ready(email, challenge);
        return SENT;
    }

    public PasswordOtpVerificationResponse verify(VerifyPasswordOtpRequest request) {
        String email = normalize(request.email());
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        if (
            !store.verify(email, codeHash(email, request.otp()), PasswordResetOtpStore.digest(token))
        ) throw new ApiException(
            HttpStatus.BAD_REQUEST,
            "Mã OTP không chính xác, đã hết hạn hoặc đã nhập sai 5 lần. Vui lòng gửi mã mới."
        );
        return new PasswordOtpVerificationResponse(token, 300);
    }

    public AuthMessageResponse reset(ResetPasswordRequest request) {
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) throw new ApiException(
            HttpStatus.BAD_REQUEST,
            "Mật khẩu quá dài. Vui lòng dùng mật khẩu ngắn hơn."
        );
        var grant = store.consume(normalize(request.email()), PasswordResetOtpStore.digest(request.token()));
        if (grant == null) throw invalidSession();
        var user = users.lockById(grant.userId()).orElseThrow(this::invalidSession);
        if (
            !"Active".equals(user.getStatus()) || user.getAuthVersion() != grant.authVersion()
        ) throw invalidSession();
        user.resetPassword(encoder.encode(request.password()));
        return new AuthMessageResponse("Đã đặt lại mật khẩu. Vui lòng đăng nhập bằng mật khẩu mới.");
    }

    private String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private ApiException invalidSession() {
        return new ApiException(
            HttpStatus.BAD_REQUEST,
            "Phiên xác minh đã hết hạn hoặc đã được sử dụng. Vui lòng yêu cầu mã OTP mới."
        );
    }

    private String codeHash(String email, String otp) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return HexFormat.of().formatHex(
                mac.doFinal(("password-reset|" + email + "|" + otp).getBytes(StandardCharsets.UTF_8))
            );
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
