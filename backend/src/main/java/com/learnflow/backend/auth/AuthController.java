package com.learnflow.backend.auth;

import com.learnflow.backend.auth.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService service;
    private final PasswordResetService passwords;

    public AuthController(AuthService service, PasswordResetService passwords) {
        this.service = service;
        this.passwords = passwords;
    }

    @PostMapping("/forgot-password")
    public AuthMessageResponse forgot(@Valid @RequestBody ForgotPasswordRequest request) {
        return passwords.forgot(request);
    }

    @PostMapping("/reset-password")
    public AuthMessageResponse reset(@Valid @RequestBody ResetPasswordRequest request) {
        return passwords.reset(request);
    }

    @PostMapping("/reset-password/verify")
    public PasswordOtpVerificationResponse verifyOtp(@Valid @RequestBody VerifyPasswordOtpRequest request) {
        return passwords.verify(request);
    }

    @PostMapping("/register/email-code")
    public EmailCodeResponse sendEmailCode(@Valid @RequestBody EmailCodeRequest request) {
        return service.sendEmailCode(request);
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return service.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return service.login(request);
    }

    @PutMapping("/me")
    public UserResponse update(Authentication a, @Valid @RequestBody ProfileRequest r) {
        return service.update((Long) a.getPrincipal(), r);
    }

    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        return service.current((Long) authentication.getPrincipal());
    }
}
