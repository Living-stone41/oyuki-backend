package com.oyuki.seller.dto;

import com.oyuki.seller.entity.SellerProfile;
import com.oyuki.user.enums.AccountStatus;
import com.oyuki.user.enums.FacialVerificationStatus;

import java.math.BigDecimal;

public record SellerProfileResponse(
        Long id,
        Long userId,
        String fullName,
        String email,
        String phoneNumber,
        AccountStatus accountStatus,
        String businessName,
        String bio,
        String profileImageUrl,
        String coverImageUrl,
        String state,
        String lga,
        String area,
        String addressLine,
        BigDecimal latitude,
        BigDecimal longitude,
        String idDocumentUrl,
        FacialVerificationStatus facialVerificationStatus,
        String bankName,
        String accountName,
        String accountNumber,
        boolean profileCompleted
) {

    public static SellerProfileResponse from(
            SellerProfile profile
    ) {
        return new SellerProfileResponse(
                profile.getId(),
                profile.getUser().getId(),
                profile.getUser().getFullName(),
                profile.getUser().getEmail(),
                profile.getUser().getPhoneNumber(),
                profile.getUser().getStatus(),
                profile.getBusinessName(),
                profile.getBio(),
                profile.getProfileImageUrl(),
                profile.getCoverImageUrl(),
                profile.getState(),
                profile.getLga(),
                profile.getArea(),
                profile.getAddressLine(),
                profile.getLatitude(),
                profile.getLongitude(),
                profile.getIdDocumentUrl(),
                profile.getFacialVerificationStatus(),
                profile.getBankName(),
                profile.getAccountName(),
                profile.getAccountNumber(),
                isComplete(profile)
        );
    }

    public static boolean isComplete(
            SellerProfile profile
    ) {
        return hasText(profile.getProfileImageUrl())
                && hasText(profile.getIdDocumentUrl());
    }

    private static boolean hasText(
            String value
    ) {
        return value != null && !value.isBlank();
    }
}
