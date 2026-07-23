package com.oyuki.seller.service;

import com.oyuki.seller.dto.SellerProfileRequest;
import com.oyuki.seller.dto.SellerProfileResponse;
import com.oyuki.seller.entity.SellerProfile;
import com.oyuki.seller.repository.SellerProfileRepository;
import com.oyuki.user.entity.User;
import com.oyuki.user.enums.AccountStatus;
import com.oyuki.user.enums.Role;
import com.oyuki.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.oyuki.common.storage.FileStorageService;
import org.springframework.web.multipart.MultipartFile;

@Service
public class SellerProfileService {
 private final SellerProfileRepository sellerProfileRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    public SellerProfileService(
            SellerProfileRepository sellerProfileRepository,
            UserRepository userRepository,
            FileStorageService fileStorageService
    ) {
        this.sellerProfileRepository = sellerProfileRepository;
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
    }

    @Transactional
    public SellerProfileResponse saveProfile(
            Long userId,
            SellerProfileRequest request
    ) {
        User user = getSeller(userId);

        SellerProfile profile = sellerProfileRepository
                .findByUserId(userId)
                .orElseGet(() ->
                        SellerProfile.builder()
                                .user(user)
                                .build()
                );

        profile.setBusinessName(request.businessName().trim());
        profile.setBio(request.bio().trim());
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

        return SellerProfileResponse.from(
                sellerProfileRepository.save(profile)
        );
    }

    @Transactional(readOnly = true)
    public SellerProfileResponse getProfile(Long userId) {
        getSeller(userId);

        SellerProfile profile = sellerProfileRepository
                .findByUserId(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Seller profile has not been completed"
                        )
                );

        return SellerProfileResponse.from(profile);
    }

    private User getSeller(Long userId) {
        User user = userRepository
                .findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "User account was not found"
                        )
                );

        if (user.getRole() != Role.SELLER) {
            throw new IllegalStateException(
                    "This endpoint is only available to sellers"
            );
        }

        if (user.getStatus() != AccountStatus.PENDING_APPROVAL
                && user.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalStateException(
                    "This seller account cannot update its profile"
            );
        }

        return user;
    }
    @Transactional
public String uploadProfileImage(
        Long userId,
        MultipartFile file
) {
    User user = getSeller(userId);
    SellerProfile profile = getCompletedProfile(userId);

    String imageUrl =
            fileStorageService.storeImage(
                    file,
                    "profiles"
            );

    profile.setProfileImageUrl(imageUrl);
    user.setProfileImageUrl(imageUrl);

    sellerProfileRepository.save(profile);
    userRepository.save(user);

    return imageUrl;
}

@Transactional
public String uploadCoverImage(
        Long userId,
        MultipartFile file
) {
    getSeller(userId);
    SellerProfile profile = getCompletedProfile(userId);

    String imageUrl =
            fileStorageService.storeImage(
                    file,
                    "covers"
            );

    profile.setCoverImageUrl(imageUrl);
    sellerProfileRepository.save(profile);

    return imageUrl;
}

@Transactional
public String uploadIdDocument(
        Long userId,
        MultipartFile file
) {
    getSeller(userId);
    SellerProfile profile = getCompletedProfile(userId);

    String documentUrl =
            fileStorageService.storeDocument(file);

    profile.setIdDocumentUrl(documentUrl);
    sellerProfileRepository.save(profile);

    return documentUrl;
}

private SellerProfile getCompletedProfile(Long userId) {
    return sellerProfileRepository
            .findByUserId(userId)
            .orElseThrow(() ->
                    new IllegalStateException(
                            "Complete your seller profile before uploading files"
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