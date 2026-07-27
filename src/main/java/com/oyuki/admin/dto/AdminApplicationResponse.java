package com.oyuki.admin.dto;

import com.oyuki.kitchen.dto.KitchenImageResponse;
import com.oyuki.kitchen.entity.KitchenImage;
import com.oyuki.kitchen.entity.KitchenProfile;
import com.oyuki.seller.entity.SellerProfile;
import com.oyuki.user.entity.User;
import com.oyuki.user.enums.AccountStatus;
import com.oyuki.user.enums.FacialVerificationStatus;
import com.oyuki.user.enums.Role;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AdminApplicationResponse(

        Long userId,
        Role role,
        String fullName,
        String email,
        String phoneNumber,
        AccountStatus accountStatus,
        String statusReason,

        String businessName,
        String bio,
        String cuisine,

        String profileImageUrl,
        String coverImageUrl,

        String state,
        String lga,
        String area,
        String addressLine,

        BigDecimal latitude,
        BigDecimal longitude,

        String idDocumentUrl,
        String businessDocumentUrl,
        String cacDocumentUrl,

        FacialVerificationStatus facialVerificationStatus,

        String bankName,
        String accountName,
        String accountNumber,

        List<KitchenImageResponse> kitchenImages,

        LocalDateTime registeredAt,
        LocalDateTime profileSubmittedAt,
        boolean profileCompleted
) {

    public static AdminApplicationResponse fromSeller(
            SellerProfile profile
    ) {
        User user = profile.getUser();

        return new AdminApplicationResponse(
                user.getId(),
                user.getRole(),
                user.getFullName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getStatus(),
                user.getStatusReason(),

                profile.getBusinessName(),
                profile.getBio(),
                null,

                profile.getProfileImageUrl(),
                profile.getCoverImageUrl(),

                profile.getState(),
                profile.getLga(),
                profile.getArea(),
                profile.getAddressLine(),

                profile.getLatitude(),
                profile.getLongitude(),

                profile.getIdDocumentUrl(),
                profile.getBusinessDocumentUrl(),
                profile.getCacDocumentUrl(),

                profile.getFacialVerificationStatus(),

                profile.getBankName(),
                profile.getAccountName(),
                profile.getAccountNumber(),

                List.of(),

                user.getCreatedAt(),
                profile.getCreatedAt(),

                hasText(profile.getProfileImageUrl())
                        && hasText(profile.getIdDocumentUrl())
                        && hasText(profile.getBusinessName())
                        && hasText(profile.getAddressLine())
        );
    }

    public static AdminApplicationResponse fromKitchen(
            KitchenProfile profile,
            List<KitchenImage> kitchenImages
    ) {
        User user = profile.getUser();

        List<KitchenImageResponse> gallery =
                kitchenImages == null
                        ? List.of()
                        : kitchenImages.stream()
                                .map(KitchenImageResponse::from)
                                .toList();

        return new AdminApplicationResponse(
                user.getId(),
                user.getRole(),
                user.getFullName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getStatus(),
                user.getStatusReason(),

                profile.getKitchenName(),
                profile.getBio(),
                profile.getCuisine(),

                profile.getProfileImageUrl(),
                profile.getCoverImageUrl(),

                profile.getState(),
                profile.getLga(),
                profile.getArea(),
                profile.getAddressLine(),

                profile.getLatitude(),
                profile.getLongitude(),

                profile.getIdDocumentUrl(),
                profile.getBusinessDocumentUrl(),
                profile.getCacDocumentUrl(),

                profile.getFacialVerificationStatus(),

                profile.getBankName(),
                profile.getAccountName(),
                profile.getAccountNumber(),

                gallery,

                user.getCreatedAt(),
                profile.getCreatedAt(),

                hasText(profile.getProfileImageUrl())
                        && hasText(profile.getIdDocumentUrl())
                        && hasText(profile.getKitchenName())
                        && hasText(profile.getAddressLine())
                        && !gallery.isEmpty()
        );
    }

    public static AdminApplicationResponse incomplete(
            User user
    ) {
        return new AdminApplicationResponse(
                user.getId(),
                user.getRole(),
                user.getFullName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getStatus(),
                user.getStatusReason(),

                null,
                null,
                null,

                user.getProfileImageUrl(),
                null,

                null,
                null,
                null,
                null,

                null,
                null,

                null,
                null,
                null,

                FacialVerificationStatus.NOT_SUBMITTED,

                null,
                null,
                null,

                List.of(),

                user.getCreatedAt(),
                null,
                false
        );
    }

    private static boolean hasText(
            String value
    ) {
        return value != null
                && !value.isBlank();
    }
}