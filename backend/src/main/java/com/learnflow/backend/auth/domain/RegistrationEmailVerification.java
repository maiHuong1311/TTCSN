package com.learnflow.backend.auth.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "registration_email_verification")
public class RegistrationEmailVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, unique = true, length = 255)
    public String email;

    @Column(name = "code_hash", nullable = false)
    public String codeHash;

    @Column(name = "expires_at", nullable = false)
    public Instant expiresAt;

    @Column(name = "sent_at", nullable = false)
    public Instant sentAt;

    @Column(name = "window_start", nullable = false)
    public Instant windowStart;

    @Column(name = "send_count", nullable = false)
    public int sendCount;

    @Column(nullable = false)
    public int attempts;

    @Column(nullable = false)
    public boolean consumed;
}
