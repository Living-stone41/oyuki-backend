package com.oyuki.user.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.oyuki.user.enums.AccountStatus;
import com.oyuki.user.enums.Role;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_users_email",
                        columnNames = "email"
                ),
                @UniqueConstraint(
                        name = "uk_users_phone_number",
                        columnNames = "phone_number"
                )
        },
        indexes = {
                @Index(
                        name = "idx_users_role",
                        columnList = "role"
                ),
                @Index(
                        name = "idx_users_status",
                        columnList = "account_status"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "full_name",
            nullable = false,
            length = 150
    )
    private String fullName;

    /*
     * Email is nullable because a user may choose to register
     * with only a phone number.
     */
    @Column(
            name = "email",
            length = 150
    )
    private String email;

    /*
     * Phone number is nullable because a user may choose to
     * register with only an email address.
     */
    @Column(
            name = "phone_number",
            length = 30
    )
    private String phoneNumber;

    /*
     * This stores the BCrypt encoded password.
     * Never store or return a plain password.
     */
    @JsonIgnore
    @Column(
            name = "password_hash",
            nullable = false,
            length = 255
    )
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "role",
            nullable = false,
            length = 30
    )
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "account_status",
            nullable = false,
            length = 30
    )
    @Builder.Default
    private AccountStatus status = AccountStatus.PENDING_VERIFICATION;

    @Column(
            name = "email_verified",
            nullable = false
    )
    @Builder.Default
    private boolean emailVerified = false;

    @Column(
            name = "phone_verified",
            nullable = false
    )
    @Builder.Default
    private boolean phoneVerified = false;

    /*
     * Admin can use this field to explain why an account
     * was rejected, suspended or disabled.
     */
    @Column(
            name = "status_reason",
            length = 500
    )
    private String statusReason;

    @Column(
            name = "profile_image_url",
            length = 500
    )
    private String profileImageUrl;

    @Column(
            name = "last_login_at"
    )
    
    
    private LocalDateTime lastLoginAt;

    @Column(
        name = "token_version",
        nullable = false
)
    @Builder.Default
    private int tokenVersion = 0;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private LocalDateTime updatedAt;

    /*
     * Runs automatically before the user is inserted.
     */
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();

        createdAt = now;
        updatedAt = now;

        if (status == null) {
            status = AccountStatus.PENDING_VERIFICATION;
        }
    }

    /*
     * Runs automatically before the user is updated.
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /*
     * Returns true when the account is allowed to log in.
     */
    public boolean isActive() {
        return status == AccountStatus.ACTIVE;
    }

    /*
     * Returns true when at least one contact method
     * has been verified.
     */
    public boolean isContactVerified() {
        return emailVerified || phoneVerified;
    }
}