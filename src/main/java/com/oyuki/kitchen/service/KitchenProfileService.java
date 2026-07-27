package com.oyuki.kitchen.service;

import com.oyuki.common.storage.FileStorageService;
import com.oyuki.kitchen.dto.*;
import com.oyuki.kitchen.entity.*;
import com.oyuki.kitchen.repository.*;
import com.oyuki.user.entity.User;
import com.oyuki.user.enums.AccountStatus;
import com.oyuki.user.enums.Role;
import com.oyuki.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@Service
public class KitchenProfileService {
    private final KitchenProfileRepository kitchenProfileRepository;
    private final KitchenImageRepository kitchenImageRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    public KitchenProfileService(KitchenProfileRepository kitchenProfileRepository,
            KitchenImageRepository kitchenImageRepository, UserRepository userRepository,
            FileStorageService fileStorageService) {
        this.kitchenProfileRepository = kitchenProfileRepository;
        this.kitchenImageRepository = kitchenImageRepository;
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
    }

    @Transactional
    public KitchenProfileResponse saveProfile(Long userId, KitchenProfileRequest request) {
        User user = getKitchen(userId);
        KitchenProfile profile = kitchenProfileRepository.findByUserId(userId)
                .orElseGet(() -> KitchenProfile.builder().user(user).build());
        profile.setKitchenName(request.kitchenName().trim());
        profile.setBio(request.bio().trim());
        profile.setCuisine(request.cuisine().trim());
        if (hasText(request.profileImageUrl())) profile.setProfileImageUrl(request.profileImageUrl().trim());
        if (hasText(request.coverImageUrl())) profile.setCoverImageUrl(request.coverImageUrl().trim());
        profile.setState(request.state().trim()); profile.setLga(request.lga().trim());
        profile.setArea(request.area().trim()); profile.setAddressLine(request.addressLine().trim());
        profile.setLatitude(request.latitude()); profile.setLongitude(request.longitude());
        if (hasText(request.idDocumentUrl())) profile.setIdDocumentUrl(request.idDocumentUrl().trim());
        profile.setBankName(clean(request.bankName())); profile.setAccountName(clean(request.accountName()));
        profile.setAccountNumber(request.accountNumber() == null ? null : request.accountNumber().replace(" ", "").trim());
        KitchenProfile saved = kitchenProfileRepository.save(profile);
        if (hasText(saved.getProfileImageUrl())) { user.setProfileImageUrl(saved.getProfileImageUrl()); userRepository.save(user); }
        return response(saved);
    }

    @Transactional(readOnly = true)
    public KitchenProfileResponse getProfile(Long userId) { getKitchen(userId); return response(getCompletedProfile(userId)); }

    @Transactional
    public String uploadProfileImage(Long userId, MultipartFile file) {
        User user = getKitchen(userId); KitchenProfile profile = getCompletedProfile(userId);
        String url = fileStorageService.storeImage(file, "profiles"); profile.setProfileImageUrl(url); user.setProfileImageUrl(url);
        kitchenProfileRepository.save(profile); userRepository.save(user); return url;
    }
    @Transactional
    public String uploadCoverImage(Long userId, MultipartFile file) {
        getKitchen(userId); KitchenProfile profile = getCompletedProfile(userId);
        String url = fileStorageService.storeImage(file, "covers"); profile.setCoverImageUrl(url); kitchenProfileRepository.save(profile); return url;
    }
    @Transactional
    public String uploadIdDocument(Long userId, MultipartFile file) {
        getKitchen(userId); KitchenProfile profile = getCompletedProfile(userId);
        String url = fileStorageService.storeDocument(file); profile.setIdDocumentUrl(url); kitchenProfileRepository.save(profile); return url;
    }
    @Transactional
    public KitchenImageResponse uploadKitchenImage(Long userId, MultipartFile file, String caption) {
        getKitchen(userId); KitchenProfile profile = getCompletedProfile(userId);
        String url = fileStorageService.storeImage(file, "kitchens");
        int order = Math.toIntExact(kitchenImageRepository.countByKitchenProfileId(profile.getId()));
        KitchenImage image = KitchenImage.builder().kitchenProfile(profile).imageUrl(url).caption(clean(caption)).displayOrder(order).build();
        return KitchenImageResponse.from(kitchenImageRepository.save(image));
    }
    @Transactional(readOnly = true)
    public List<KitchenImageResponse> getKitchenImages(Long userId) {
        getKitchen(userId); KitchenProfile profile = getCompletedProfile(userId);
        return kitchenImageRepository.findAllByKitchenProfileIdOrderByDisplayOrderAscIdAsc(profile.getId())
                .stream().map(KitchenImageResponse::from).toList();
    }
    @Transactional
    public void deleteKitchenImage(Long userId, Long imageId) {
        getKitchen(userId); KitchenProfile profile = getCompletedProfile(userId);
        KitchenImage image = kitchenImageRepository.findById(imageId)
                .orElseThrow(() -> new IllegalArgumentException("Kitchen image was not found"));
        if (!image.getKitchenProfile().getId().equals(profile.getId())) throw new IllegalStateException("You cannot delete another kitchen's image");
        kitchenImageRepository.delete(image);
    }

    private KitchenProfileResponse response(KitchenProfile p) {
        return KitchenProfileResponse.from(p, kitchenImageRepository.findAllByKitchenProfileIdOrderByDisplayOrderAscIdAsc(p.getId()));
    }
    private User getKitchen(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User account was not found"));
        if (user.getRole() != Role.KITCHEN) throw new IllegalStateException("This endpoint is only available to kitchens");
        if (user.getStatus() != AccountStatus.PENDING_APPROVAL && user.getStatus() != AccountStatus.ACTIVE)
            throw new IllegalStateException("This kitchen account cannot update its profile");
        return user;
    }
    private KitchenProfile getCompletedProfile(Long userId) {
        return kitchenProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Complete your kitchen profile before uploading files"));
    }
    private boolean hasText(String v) { return v != null && !v.isBlank(); }
    private String clean(String v) { return hasText(v) ? v.trim() : null; }
}
