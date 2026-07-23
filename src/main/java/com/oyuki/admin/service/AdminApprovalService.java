package com.oyuki.admin.service;

import com.oyuki.admin.dto.AdminApplicationResponse;
import com.oyuki.admin.dto.RejectApplicationRequest;
import com.oyuki.kitchen.entity.KitchenProfile;
import com.oyuki.kitchen.repository.KitchenProfileRepository;
import com.oyuki.seller.entity.SellerProfile;
import com.oyuki.seller.repository.SellerProfileRepository;
import com.oyuki.user.entity.User;
import com.oyuki.user.enums.AccountStatus;
import com.oyuki.user.enums.Role;
import com.oyuki.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class AdminApprovalService {

    private final UserRepository userRepository;
    private final SellerProfileRepository sellerProfileRepository;
    private final KitchenProfileRepository kitchenProfileRepository;

    public AdminApprovalService(
            UserRepository userRepository,
            SellerProfileRepository sellerProfileRepository,
            KitchenProfileRepository kitchenProfileRepository
    ) {
        this.userRepository = userRepository;
        this.sellerProfileRepository = sellerProfileRepository;
        this.kitchenProfileRepository = kitchenProfileRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminApplicationResponse> getPendingApplications() {

        List<User> pendingSellers =
                userRepository.findAllByRoleAndStatus(
                        Role.SELLER,
                        AccountStatus.PENDING_APPROVAL
                );

        List<User> pendingKitchens =
                userRepository.findAllByRoleAndStatus(
                        Role.KITCHEN,
                        AccountStatus.PENDING_APPROVAL
                );

        List<AdminApplicationResponse> applications =
                new ArrayList<>();

        pendingSellers
                .stream()
                .map(this::convertToResponse)
                .forEach(applications::add);

        pendingKitchens
                .stream()
                .map(this::convertToResponse)
                .forEach(applications::add);

        applications.sort(
                Comparator.comparing(
                        AdminApplicationResponse::registeredAt,
                        Comparator.nullsLast(
                                Comparator.reverseOrder()
                        )
                )
        );

        return applications;
    }

    @Transactional(readOnly = true)
    public AdminApplicationResponse getApplication(
            Long userId
    ) {
        User user = getSellerOrKitchen(userId);

        return convertToResponse(user);
    }

    @Transactional
    public AdminApplicationResponse approveApplication(
            Long userId
    ) {
        User user = getSellerOrKitchen(userId);

        validatePendingApplication(user);
        validateProfileCompleted(user);

        user.setStatus(AccountStatus.ACTIVE);
        user.setStatusReason(null);

        User savedUser = userRepository.save(user);

        return convertToResponse(savedUser);
    }

    @Transactional
    public AdminApplicationResponse rejectApplication(
            Long userId,
            RejectApplicationRequest request
    ) {
        User user = getSellerOrKitchen(userId);

        validatePendingApplication(user);

        user.setStatus(AccountStatus.REJECTED);
        user.setStatusReason(request.reason().trim());

        /*
         * This invalidates JWTs issued before rejection.
         */
        user.setTokenVersion(user.getTokenVersion() + 1);

        User savedUser = userRepository.save(user);

        return convertToResponse(savedUser);
    }

    private User getSellerOrKitchen(Long userId) {

        User user = userRepository
                .findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Application was not found"
                        )
                );

        if (user.getRole() != Role.SELLER
                && user.getRole() != Role.KITCHEN) {

            throw new IllegalStateException(
                    "This user is not a seller or kitchen"
            );
        }

        return user;
    }

    private void validatePendingApplication(User user) {

        if (user.getStatus() !=
                AccountStatus.PENDING_APPROVAL) {

            throw new IllegalStateException(
                    "This application is not awaiting approval"
            );
        }
    }

    private void validateProfileCompleted(User user) {

        if (user.getRole() == Role.SELLER) {
            SellerProfile profile =
                    sellerProfileRepository
                            .findByUserId(user.getId())
                            .orElseThrow(() ->
                                    new IllegalStateException(
                                            "The seller must complete their profile before approval"
                                    )
                            );

            requireApprovalFiles(
                    profile.getProfileImageUrl(),
                    profile.getIdDocumentUrl()
            );
            return;
        }

        KitchenProfile profile =
                kitchenProfileRepository
                        .findByUserId(user.getId())
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "The kitchen must complete its profile before approval"
                                )
                        );

        requireApprovalFiles(
                profile.getProfileImageUrl(),
                profile.getIdDocumentUrl()
        );
    }

    private void requireApprovalFiles(
            String profileImageUrl,
            String idDocumentUrl
    ) {
        if (profileImageUrl == null
                || profileImageUrl.isBlank()) {
            throw new IllegalStateException(
                    "A profile picture is required before approval"
            );
        }

        if (idDocumentUrl == null
                || idDocumentUrl.isBlank()) {
            throw new IllegalStateException(
                    "An identification document is required before approval"
            );
        }
    }

    private AdminApplicationResponse convertToResponse(
            User user
    ) {
        if (user.getRole() == Role.SELLER) {

            return sellerProfileRepository
                    .findByUserId(user.getId())
                    .map(AdminApplicationResponse::fromSeller)
                    .orElseGet(() ->
                            AdminApplicationResponse.incomplete(user)
                    );
        }

        if (user.getRole() == Role.KITCHEN) {

            return kitchenProfileRepository
                    .findByUserId(user.getId())
                    .map(AdminApplicationResponse::fromKitchen)
                    .orElseGet(() ->
                            AdminApplicationResponse.incomplete(user)
                    );
        }

        throw new IllegalStateException(
                "Unsupported application role"
        );
    }
}