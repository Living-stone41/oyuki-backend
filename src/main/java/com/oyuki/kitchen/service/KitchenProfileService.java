package com.oyuki.kitchen.service;

import com.oyuki.kitchen.dto.KitchenProfileRequest;
import com.oyuki.kitchen.dto.KitchenProfileResponse;
import com.oyuki.kitchen.entity.KitchenProfile;
import com.oyuki.kitchen.repository.KitchenProfileRepository;
import com.oyuki.user.entity.User;
import com.oyuki.user.enums.AccountStatus;
import com.oyuki.user.enums.Role;
import com.oyuki.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.oyuki.common.storage.FileStorageService;
import org.springframework.web.multipart.MultipartFile;

@Service
public class KitchenProfileService {

    private final KitchenProfileRepository kitchenProfileRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    public KitchenProfileService(
            KitchenProfileRepository kitchenProfileRepository,
            UserRepository userRepository,
            FileStorageService fileStorageService
    ) {
        this.kitchenProfileRepository = kitchenProfileRepository;
        this.userRepository = userRepository;
    this.fileStorageService = fileStorageService;
}

    @Transactional
    public KitchenProfileResponse saveProfile(
            Long userId,
            KitchenProfileRequest request
    ) {
        User user = getKitchen(userId);

        KitchenProfile profile = kitchenProfileRepository
                .findByUserId(userId)
                .orElseGet(() ->
                        KitchenProfile.builder()
                                .user(user)
                                .build()
                );

        profile.setKitchenName(request.kitchenName().trim());
        profile.setBio(request.bio().trim());
        profile.setCuisine(request.cuisine().trim());
        if (hasText(request.profileImageUrl())) {
            profile.setProfileImageUrl(request.profileImageUrl().trim());
        }

        if (hasText(request.coverImageUrl())) {
            profile.setCoverImageUrl(request.coverImageUrl().trim());
        }
        profile.setState(request.state().trim());
        profile.setLga(request.lga().trim());
        profile.setArea(request.area().trim());
        profile.setAddressLine(request.addressLine().trim());
        profile.setLatitude(request.latitude());
        profile.setLongitude(request.longitude());
        if (hasText(request.idDocumentUrl())) {
            profile.setIdDocumentUrl(request.idDocumentUrl().trim());
        }

        profile.setBankName(clean(request.bankName()));
        profile.setAccountName(clean(request.accountName()));
        profile.setAccountNumber(
                request.accountNumber() == null
                        ? null
                        : request.accountNumber()
                                .replace(" ", "")
                                .trim()
        );

        if (hasText(profile.getProfileImageUrl())) {
            user.setProfileImageUrl(profile.getProfileImageUrl());
            userRepository.save(user);
        }

        return KitchenProfileResponse.from(
                kitchenProfileRepository.save(profile)
        );
    }

    @Transactional(readOnly = true)
    public KitchenProfileResponse getProfile(Long userId) {
        getKitchen(userId);

        KitchenProfile profile = kitchenProfileRepository
                .findByUserId(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Kitchen profile has not been completed"
                        )
                );

        return KitchenProfileResponse.from(profile);
    }

    private User getKitchen(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "User account was not found"
                        )
                );

        if (user.getRole() != Role.KITCHEN) {
            throw new IllegalStateException(
                    "This endpoint is only available to kitchens"
            );
        }

        if (user.getStatus() != AccountStatus.PENDING_APPROVAL
                && user.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalStateException(
                    "This kitchen account cannot update its profile"
            );
        }

        return user;
    }
    @Transactional
public String uploadProfileImage(
        Long userId,
        MultipartFile file
) {
    User user = getKitchen(userId);
    KitchenProfile profile = getCompletedProfile(userId);

    String imageUrl = fileStorageService.storeImage(
            file,
            "profiles"
    );

    profile.setProfileImageUrl(imageUrl);
    user.setProfileImageUrl(imageUrl);

    kitchenProfileRepository.save(profile);
    userRepository.save(user);

    return imageUrl;
}

@Transactional
public String uploadCoverImage(
        Long userId,
        MultipartFile file
) {
    getKitchen(userId);

    KitchenProfile profile =
            getCompletedProfile(userId);

    String imageUrl = fileStorageService.storeImage(
            file,
            "covers"
    );

    profile.setCoverImageUrl(imageUrl);
    kitchenProfileRepository.save(profile);

    return imageUrl;
}

@Transactional
public String uploadIdDocument(
        Long userId,
        MultipartFile file
) {
    getKitchen(userId);

    KitchenProfile profile =
            getCompletedProfile(userId);

    String documentUrl =
            fileStorageService.storeDocument(file);

    profile.setIdDocumentUrl(documentUrl);
    kitchenProfileRepository.save(profile);

    return documentUrl;
}

private KitchenProfile getCompletedProfile(Long userId) {
    return kitchenProfileRepository
            .findByUserId(userId)
            .orElseThrow(() ->
                    new IllegalStateException(
                            "Complete your kitchen profile before uploading files"
                    )
            );
}

private boolean hasText(
        String value
) {
    return value != null && !value.isBlank();
}

private String clean(
        String value
) {
    return hasText(value)
            ? value.trim()
            : null;
}
}