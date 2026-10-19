package com.learnflow.backend.auth;

import com.learnflow.backend.auth.domain.RegistrationEmailVerification;
import com.learnflow.backend.auth.dto.EmailCodeResponse;
import com.learnflow.backend.common.web.ApiException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class EmailVerificationService {

    private final RegistrationEmailVerificationRepository repository;
    private final RegistrationMailService mail;
    private final PasswordEncoder encoder;
    private final Clock clock;
    private final Set<String> blockedDomains;
    private final SecureRandom random = new SecureRandom();

    public EmailVerificationService(
        RegistrationEmailVerificationRepository repository,
        RegistrationMailService mail,
        PasswordEncoder encoder,
        Clock clock,
        @Value(
            "${application.mail.blocked-domains:mailinator.com,guerrillamail.com,10minutemail.com,tempmail.com,yopmail.com,dispostable.com}"
        ) String blockedDomains
    ) {
        this.repository = repository;
        this.mail = mail;
        this.encoder = encoder;
        this.clock = clock;
        this.blockedDomains = new HashSet<>(
            Arrays.asList(blockedDomains.toLowerCase(Locale.ROOT).split(","))
        );
    }

    public EmailCodeResponse send(String email) {
        String domain = email.substring(email.lastIndexOf('@') + 1);
        if (
            blockedDomains
                .stream()
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .anyMatch(d -> domain.equals(d) || domain.endsWith("." + d))
        ) throw new ApiException(
            HttpStatus.BAD_REQUEST,
            "Vui lòng dùng email cá nhân, không dùng email tạm thời."
        );
        var now = clock.instant();
        var v = repository.lockByEmail(email).orElseGet(RegistrationEmailVerification::new);
        if (v.sentAt != null && now.isBefore(v.sentAt.plusSeconds(60))) throw new ApiException(
            HttpStatus.TOO_MANY_REQUESTS,
            "Vui lòng chờ 60 giây giữa hai lần gửi mã."
        );
        if (v.windowStart == null || !now.isBefore(v.windowStart.plus(Duration.ofDays(1)))) {
            v.windowStart = now;
            v.sendCount = 0;
        }
        if (v.sendCount >= 10) throw new ApiException(
            HttpStatus.TOO_MANY_REQUESTS,
            "Email này đã đạt giới hạn gửi mã trong ngày. Vui lòng thử lại sau."
        );
        String code;
        do {
            code = String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000));
        } while (v.codeHash != null && encoder.matches(code, v.codeHash));
        v.email = email;
        v.codeHash = encoder.encode(code);
        v.sentAt = now;
        v.expiresAt = now.plusSeconds(600);
        v.attempts = 0;
        v.consumed = false;
        v.sendCount++;
        repository.saveAndFlush(v);
        mail.sendCode(email, code);
        return new EmailCodeResponse(
            "Đã gửi mã xác minh. Vui lòng kiểm tra hộp thư và thư rác.",
            v.expiresAt,
            now.plusSeconds(60)
        );
    }

    // Caller commits failed-attempt counters even when verification returns a 400.
    @Transactional(noRollbackFor = ApiException.class)
    public void consume(String email, String code) {
        var v = repository
            .lockByEmail(email)
            .orElseThrow(() ->
                new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Vui lòng yêu cầu mã xác minh email trước khi đăng ký."
                )
            );
        if (v.consumed || !clock.instant().isBefore(v.expiresAt)) throw new ApiException(
            HttpStatus.BAD_REQUEST,
            "Mã xác minh đã hết hạn hoặc đã được sử dụng. Vui lòng gửi lại mã."
        );
        if (v.attempts >= 5) throw new ApiException(
            HttpStatus.BAD_REQUEST,
            "Bạn đã nhập sai mã quá 5 lần. Vui lòng gửi lại mã."
        );
        v.attempts++;
        if (!encoder.matches(code, v.codeHash)) throw new ApiException(
            HttpStatus.BAD_REQUEST,
            "Mã xác minh không chính xác. Vui lòng kiểm tra lại."
        );
        v.consumed = true;
    }
}
