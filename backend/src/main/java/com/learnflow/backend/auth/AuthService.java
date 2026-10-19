package com.learnflow.backend.auth;

import com.learnflow.backend.auth.domain.*;
import com.learnflow.backend.auth.dto.*;
import com.learnflow.backend.common.web.ApiException;
import java.time.Clock;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {

    private final AuthRepository repository;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final Clock clock;
    private final EmailVerificationService verification;

    public AuthService(
        AuthRepository repository,
        PasswordEncoder encoder,
        JwtService jwt,
        Clock clock,
        EmailVerificationService verification
    ) {
        this.repository = repository;
        this.encoder = encoder;
        this.jwt = jwt;
        this.clock = clock;
        this.verification = verification;
    }

    public EmailCodeResponse sendEmailCode(EmailCodeRequest request) {
        String email = request.email().trim().toLowerCase(java.util.Locale.ROOT);
        if (repository.existsByEmailIgnoreCase(email)) throw new ApiException(
            HttpStatus.CONFLICT,
            "Email is already registered"
        );
        return verification.send(email);
    }

    @Transactional(noRollbackFor = ApiException.class)
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase(java.util.Locale.ROOT);
        if (repository.existsByEmailIgnoreCase(email)) throw new ApiException(
            HttpStatus.CONFLICT,
            "Email is already registered"
        );
        if (repository.existsByUsernameIgnoreCase(request.username())) throw new ApiException(
            HttpStatus.CONFLICT,
            "Username is already registered"
        );
        verification.consume(email, request.verificationCode());
        AppUser user = new AppUser(
            email,
            encoder.encode(request.password()),
            request.fullName().trim(),
            Role.Customer,
            clock.instant()
        );
        user.setUsername(request.username());
        user.updateProfile(request.fullName().trim(), request.phoneNumber(), null, null, null);
        repository.save(user);
        return response(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        AppUser user = repository
            .findByEmailIgnoreCase(request.identifier().trim())
            .or(() -> repository.findByUsernameIgnoreCase(request.identifier().trim()))
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        if (!encoder.matches(request.password(), user.getPasswordHash())) throw new ApiException(
            HttpStatus.UNAUTHORIZED,
            "Invalid email or password"
        );
        if (!"Active".equals(user.getStatus())) throw new ApiException(
            HttpStatus.FORBIDDEN,
            "Account is blocked"
        );
        return response(user);
    }

    @Transactional(readOnly = true)
    public UserResponse current(Long userId) {
        return toResponse(requireUser(userId));
    }

    @Transactional(readOnly = true)
    public AppUser requireUser(Long userId) {
        AppUser user = repository
            .findById(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User no longer exists"));
        if (!"Active".equals(user.getStatus())) throw new ApiException(
            HttpStatus.FORBIDDEN,
            "Account is blocked"
        );
        return user;
    }

    public UserResponse update(Long id, ProfileRequest r) {
        AppUser u = requireUser(id);
        u.updateProfile(r.fullName(), r.phoneNumber(), r.heightCm(), r.weightKg(), r.preferredStyle());
        return toResponse(u);
    }

    public java.util.List<UserResponse> users() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    public UserResponse status(Long actor, Long id, String status) {
        if (actor.equals(id)) throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot block your own account");
        if (!java.util.Set.of("Active", "Blocked").contains(status)) throw new ApiException(
            HttpStatus.BAD_REQUEST,
            "Invalid status"
        );
        AppUser u = repository
            .findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        u.setStatus(status);
        return toResponse(u);
    }

    public AppUser lockUser(Long id) {
        return repository
            .lockById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    public void bootstrapAdmin(String email, String password) {
        if (repository.existsByEmailIgnoreCase(email)) return;
        if (password.length() < 12) throw new IllegalArgumentException(
            "Local admin password must have at least 12 characters"
        );
        AppUser u = new AppUser(
            email,
            encoder.encode(password),
            "Quản trị viên",
            Role.Admin,
            clock.instant()
        );
        u.setUsername("admin_" + java.util.UUID.randomUUID().toString().substring(0, 8));
        repository.save(u);
    }

    public String displayName(Long id) {
        return repository.findById(id).map(AppUser::getFullName).orElse("Khách hàng trước đây");
    }

    private AuthResponse response(AppUser user) {
        JwtService.Token token = jwt.issue(user);
        return new AuthResponse(token.value(), "Bearer", token.expiresAt(), toResponse(user));
    }

    private UserResponse toResponse(AppUser user) {
        return new UserResponse(
            user.getId(),
            user.getEmail(),
            user.getFullName(),
            user.getUsername(),
            user.getPhoneNumber(),
            user.getRole().name(),
            user.getStatus(),
            user.getCreatedAt(),
            user.getHeightCm(),
            user.getWeightKg(),
            user.getPreferredStyle()
        );
    }
}
