package com.oyuki.kitchen.dto;

import com.oyuki.kitchen.entity.KitchenImage;
import com.oyuki.kitchen.entity.KitchenProfile;
import com.oyuki.user.enums.AccountStatus;
import com.oyuki.user.enums.FacialVerificationStatus;
import java.math.BigDecimal;
import java.util.List;

public record KitchenProfileResponse(
        Long id, Long userId, String fullName, String email, String phoneNumber,
        AccountStatus accountStatus, String kitchenName, String bio, String cuisine,
        String profileImageUrl, String coverImageUrl, String state, String lga, String area,
        String addressLine, BigDecimal latitude, BigDecimal longitude, String idDocumentUrl,
        FacialVerificationStatus facialVerificationStatus, String bankName, String accountName,
        String accountNumber, List<KitchenImageResponse> kitchenImages, boolean profileCompleted
) {
    public static KitchenProfileResponse from(KitchenProfile profile, List<KitchenImage> images) {
        List<KitchenImageResponse> gallery = images == null ? List.of() : images.stream().map(KitchenImageResponse::from).toList();
        return new KitchenProfileResponse(
                profile.getId(), profile.getUser().getId(), profile.getUser().getFullName(),
                profile.getUser().getEmail(), profile.getUser().getPhoneNumber(), profile.getUser().getStatus(),
                profile.getKitchenName(), profile.getBio(), profile.getCuisine(), profile.getProfileImageUrl(),
                profile.getCoverImageUrl(), profile.getState(), profile.getLga(), profile.getArea(),
                profile.getAddressLine(), profile.getLatitude(), profile.getLongitude(), profile.getIdDocumentUrl(),
                profile.getFacialVerificationStatus(), profile.getBankName(), profile.getAccountName(),
                profile.getAccountNumber(), gallery, isComplete(profile, gallery)
        );
    }

    public static boolean isComplete(KitchenProfile profile, List<KitchenImageResponse> images) {
        return hasText(profile.getKitchenName()) && hasText(profile.getBio()) && hasText(profile.getCuisine())
                && hasText(profile.getProfileImageUrl()) && hasText(profile.getIdDocumentUrl())
                && hasText(profile.getState()) && hasText(profile.getLga()) && hasText(profile.getArea())
                && hasText(profile.getAddressLine()) && images != null && !images.isEmpty();
    }
    private static boolean hasText(String value) { return value != null && !value.isBlank(); }
}
