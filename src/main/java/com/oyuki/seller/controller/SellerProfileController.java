package com.oyuki.seller.controller;

import com.oyuki.common.response.ApiResponse;
import com.oyuki.seller.dto.SellerProfileRequest;
import com.oyuki.seller.dto.SellerProfileResponse;
import com.oyuki.seller.service.SellerProfileService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/seller/profile")
public class SellerProfileController {

    private final SellerProfileService sellerProfileService;

    public SellerProfileController(
            SellerProfileService sellerProfileService
    ) {
        this.sellerProfileService = sellerProfileService;
    }

    @PutMapping
    public ApiResponse<SellerProfileResponse> saveProfile(
            Authentication authentication,
            @Valid @RequestBody SellerProfileRequest request
    ) {
        Long userId = (Long) authentication.getPrincipal();

        return ApiResponse.success(
                "Seller profile saved successfully",
                sellerProfileService.saveProfile(userId, request)
        );
    }

    @GetMapping
    public ApiResponse<SellerProfileResponse> getProfile(
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();

        return ApiResponse.success(
                "Seller profile retrieved successfully",
                sellerProfileService.getProfile(userId)
        );
    }
    @PostMapping(
        value = "/profile-image",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE
)
public ApiResponse<Map<String, String>> uploadProfileImage(
        Authentication authentication,
        @RequestParam("file") MultipartFile file
) {
    Long userId = (Long) authentication.getPrincipal();

    String imageUrl =
            sellerProfileService.uploadProfileImage(
                    userId,
                    file
            );

    return ApiResponse.success(
            "Profile image uploaded successfully",
            Map.of("url", imageUrl)
    );
}

@PostMapping(
        value = "/cover-image",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE
)
public ApiResponse<Map<String, String>> uploadCoverImage(
        Authentication authentication,
        @RequestParam("file") MultipartFile file
) {
    Long userId = (Long) authentication.getPrincipal();

    String imageUrl =
            sellerProfileService.uploadCoverImage(
                    userId,
                    file
            );

    return ApiResponse.success(
            "Cover image uploaded successfully",
            Map.of("url", imageUrl)
    );
}

@PostMapping(
        value = "/id-document",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE
)
public ApiResponse<Map<String, String>> uploadIdDocument(
        Authentication authentication,
        @RequestParam("file") MultipartFile file
) {
    Long userId = (Long) authentication.getPrincipal();

    String documentUrl =
            sellerProfileService.uploadIdDocument(
                    userId,
                    file
            );

    return ApiResponse.success(
            "Identification document uploaded successfully",
            Map.of("url", documentUrl)
    );
}
}