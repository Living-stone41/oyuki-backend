package com.oyuki.referral.entity;

import com.oyuki.referral.enums.ReferralStatus;
import com.oyuki.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "referrals",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_referrals_referred_user",
                columnNames = "referred_user_id"
        ),
        indexes = {
                @Index(name = "idx_referrals_referrer", columnList = "referrer_id"),
                @Index(name = "idx_referrals_code", columnList = "referral_code"),
                @Index(name = "idx_referrals_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Referral {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "referrer_id", nullable = false)
    private User referrer;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "referred_user_id", nullable = false, unique = true)
    private User referredUser;

    @Column(name = "referral_code", nullable = false, length = 50)
    private String referralCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ReferralStatus status = ReferralStatus.PENDING;

    @Column(name = "referrer_reward", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal referrerReward = BigDecimal.ZERO;

    @Column(name = "referred_user_reward", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal referredUserReward = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "qualified_at")
    private LocalDateTime qualifiedAt;

    @Column(name = "rewarded_at")
    private LocalDateTime rewardedAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (status == null) status = ReferralStatus.PENDING;
        if (referrerReward == null) referrerReward = BigDecimal.ZERO;
        if (referredUserReward == null) referredUserReward = BigDecimal.ZERO;
    }
}
