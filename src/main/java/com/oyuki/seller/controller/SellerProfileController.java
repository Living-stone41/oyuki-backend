package com.oyuki.seller.controller;

import com.oyuki.common.response.ApiResponse;
import com.oyuki.seller.dto.SellerProfileRequest;
import com.oyuki.seller.dto.SellerProfileResponse;
import com.oyuki.seller.service.SellerProfileService;

import jakarta.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/seller/profile")
public class SellerProfileController {

    private final SellerProfileService sellerProfileService;

    public SellerProfileController(
            SellerProfileService sellerProfileService
    ) {
        this.sellerProfileService =
                sellerProfileService;
    }

    /*
     * Create or update the seller profile details.
     */
    @PutMapping
    public ApiResponse<SellerProfileResponse> saveProfile(
            Authentication authentication,
            @Valid
            @RequestBody
            SellerProfileRequest request
    ) {
        Long userId =
                getAuthenticatedUserId(
                        authentication
                );

        return ApiResponse.success(
                "Seller profile saved successfully",
                sellerProfileService.saveProfile(
                        userId,
                        request
                )
        );
    }

    /*
     * Get the logged-in seller's profile.
     */
    @GetMapping
    public ApiResponse<SellerProfileResponse> getProfile(
            Authentication authentication
    ) {
        Long userId =
                getAuthenticatedUserId(
                        authentication
                );

        return ApiResponse.success(
                "Seller profile retrieved successfully",
                sellerProfileService.getProfile(
                        userId
                )
        );
    }

    /*
     * Upload the seller's profile image.
     */
    @PostMapping(
            value = "/profile-image",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ApiResponse<Map<String, String>> uploadProfileImage(
            Authentication authentication,
            @RequestParam("file")
            MultipartFile file
    ) {
        Long userId =
                getAuthenticatedUserId(
                        authentication
                );

        String imageUrl =
                sellerProfileService
                        .uploadProfileImage(
                                userId,
                                file
                        );

        return ApiResponse.success(
                "Profile image uploaded successfully",
                Map.of(
                        "url",
                        imageUrl
                )
        );
    }

    /*
     * Upload the seller's cover image.
     */
    @PostMapping(
            value = "/cover-image",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ApiResponse<Map<String, String>> uploadCoverImage(
            Authentication authentication,
            @RequestParam("file")
            MultipartFile file
    ) {
        Long userId =
                getAuthenticatedUserId(
                        authentication
                );

        String imageUrl =
                sellerProfileService
                        .uploadCoverImage(
                                userId,
                                file
                        );

        return ApiResponse.success(
                "Cover image uploaded successfully",
                Map.of(
                        "url",
                        imageUrl
                )
        );
    }

    /*
     * Upload a government-issued identification document.
     */
    @PostMapping(
            value = "/id-document",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ApiResponse<Map<String, String>> uploadIdDocument(
            Authentication authentication,
            @RequestParam("file")
            MultipartFile file
    ) {
        Long userId =
                getAuthenticatedUserId(
                        authentication
                );

        String documentUrl =
                sellerProfileService
                        .uploadIdDocument(
                                userId,
                                file
                        );

        return ApiResponse.success(
                "Identification document uploaded successfully",
                Map.of(
                        "url",
                        documentUrl
                )
        );
    }

    /*
     * Upload a general business registration document.
     */
    @PostMapping(
            value = "/business-document",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ApiResponse<Map<String, String>> uploadBusinessDocument(
            Authentication authentication,
            @RequestParam("file")
            MultipartFile file
    ) {
        Long userId =
                getAuthenticatedUserId(
                        authentication
                );

        String documentUrl =
                sellerProfileService
                        .uploadBusinessDocument(
                                userId,
                                file
                        );

        return ApiResponse.success(
                "Business document uploaded successfully",
                Map.of(
                        "url",
                        documentUrl
                )
        );
    }

    /*
     * Upload a CAC registration document.
     */
    @PostMapping(
            value = "/cac-document",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ApiResponse<Map<String, String>> uploadCacDocument(
            Authentication authentication,
            @RequestParam("file")
            MultipartFile file
    ) {
        Long userId =
                getAuthenticatedUserId(
                        authentication
                );

        String documentUrl =
                sellerProfileService
                        .uploadCacDocument(
                                userId,
                                file
                        );

        return ApiResponse.success(
                "CAC document uploaded successfully",
                Map.of(
                        "url",
                        documentUrl
                )
        );
    }

    /*
     * Reads the user ID stored by the JWT authentication filter.
     */
    private Long getAuthenticatedUserId(
            Authentication authentication
    ) {
        if (
                authentication == null ||
                !authentication.isAuthenticated() ||
                authentication.getPrincipal() == null
        ) {
            throw new IllegalStateException(
                    "Authentication is required"
            );
        }

        Object principal =
                authentication.getPrincipal();

        if (principal instanceof Long userId) {
            return userId;
        }

        if (principal instanceof Integer userId) {
            return userId.longValue();
        }

        try {
            return Long.valueOf(
                    principal.toString()
            );
        } catch (NumberFormatException exception) {
            throw new IllegalStateException(
                    "The authenticated user ID is invalid"
            );
        }
    }
}