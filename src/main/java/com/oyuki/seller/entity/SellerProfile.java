package com.oyuki.seller.entity;

import com.oyuki.user.entity.User;
import com.oyuki.user.enums.FacialVerificationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "seller_profiles",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_seller_profiles_user",
                        columnNames = "user_id"
                )
        },
        indexes = {
                @Index(
                        name = "idx_seller_profiles_state",
                        columnList = "state"
                ),
                @Index(
                        name = "idx_seller_profiles_lga",
                        columnList = "lga"
                ),
                @Index(
                        name = "idx_seller_profiles_facial_status",
                        columnList = "facial_verification_status"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "user_id",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(
                    name = "fk_seller_profile_user"
            )
    )
    private User user;

    @Column(
            name = "business_name",
            nullable = false,
            length = 150
    )
    private String businessName;

    @Column(
            name = "bio",
            nullable = false,
            length = 1500
    )
    private String bio;

    @Column(
            name = "profile_image_url",
            length = 500
    )
    private String profileImageUrl;

    @Column(
            name = "cover_image_url",
            length = 500
    )
    private String coverImageUrl;

    @Column(
            name = "state",
            nullable = false,
            length = 100
    )
    private String state;

    @Column(
            name = "lga",
            nullable = false,
            length = 100
    )
    private String lga;

    @Column(
            name = "area",
            nullable = false,
            length = 150
    )
    private String area;

    @Column(
            name = "address_line",
            nullable = false,
            length = 500
    )
    private String addressLine;

    @Column(
            name = "latitude",
            precision = 10,
            scale = 7
    )
    private BigDecimal latitude;

    @Column(
            name = "longitude",
            precision = 10,
            scale = 7
    )
    private BigDecimal longitude;

    /*
     * Government-issued identification document,
     * such as NIN slip, driver's licence or passport.
     */
    @Column(
            name = "id_document_url",
            length = 500
    )
    private String idDocumentUrl;

    /*
     * Optional general business registration document.
     */
    @Column(
            name = "business_document_url",
            length = 500
    )
    private String businessDocumentUrl;

    /*
     * Optional CAC registration document.
     */
    @Column(
            name = "cac_document_url",
            length = 500
    )
    private String cacDocumentUrl;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "facial_verification_status",
            nullable = false,
            length = 30
    )
    @Builder.Default
    private FacialVerificationStatus facialVerificationStatus =
            FacialVerificationStatus.NOT_SUBMITTED;

    @Column(
            name = "bank_name",
            length = 150
    )
    private String bankName;

    @Column(
            name = "account_name",
            length = 150
    )
    private String accountName;

    @Column(
            name = "account_number",
            length = 30
    )
    private String accountNumber;

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

    @PrePersist
    protected void onCreate() {
        LocalDateTime now =
                LocalDateTime.now();

        createdAt = now;
        updatedAt = now;

        if (facialVerificationStatus == null) {
            facialVerificationStatus =
                    FacialVerificationStatus.NOT_SUBMITTED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt =
                LocalDateTime.now();
    }

    public boolean hasRequiredApprovalFiles() {
        return hasText(profileImageUrl)
                && hasText(idDocumentUrl);
    }

    public boolean isProfileComplete() {
        return hasText(businessName)
                && hasText(bio)
                && hasText(profileImageUrl)
                && hasText(idDocumentUrl)
                && hasText(state)
                && hasText(lga)
                && hasText(area)
                && hasText(addressLine);
    }

    private boolean hasText(
            String value
    ) {
        return value != null
                && !value.isBlank();
    }
}