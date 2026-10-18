package com.learnflow.backend.auth.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "auth_version", nullable = false)
    private long authVersion;

    public long getAuthVersion() {
        return authVersion;
    }

    public void resetPassword(String hash) {
        passwordHash = hash;
        authVersion++;
    }

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR)
    @Column(nullable = false)
    private Role role;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "phone_number", length = 15)
    private String phoneNumber;

    @Column(nullable = false, length = 20)
    private String status = "Active";

    @Column(name = "height_cm", precision = 5, scale = 2)
    private java.math.BigDecimal heightCm;

    @Column(name = "weight_kg", precision = 5, scale = 2)
    private java.math.BigDecimal weightKg;

    @Column(name = "preferred_style", length = 100)
    private String preferredStyle;

    public String getUsername() {
        return username;
    }

    public void setUsername(String v) {
        username = v;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String v) {
        status = v;
    }

    public void setRole(Role v) {
        role = v;
    }

    public java.math.BigDecimal getHeightCm() {
        return heightCm;
    }

    public java.math.BigDecimal getWeightKg() {
        return weightKg;
    }

    public String getPreferredStyle() {
        return preferredStyle;
    }

    public void updateProfile(
        String name,
        String phone,
        java.math.BigDecimal height,
        java.math.BigDecimal weight,
        String style
    ) {
        fullName = name;
        phoneNumber = phone;
        heightCm = height;
        weightKg = weight;
        preferredStyle = style;
    }

    protected AppUser() {}

    public AppUser(String email, String passwordHash, String fullName, Role role, Instant createdAt) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.role = role;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public Role getRole() {
        return role;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
