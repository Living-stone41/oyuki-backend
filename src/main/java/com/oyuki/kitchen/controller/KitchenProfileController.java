package com.oyuki.kitchen.controller;

import com.oyuki.common.response.ApiResponse;
import com.oyuki.kitchen.dto.KitchenProfileRequest;
import com.oyuki.kitchen.dto.KitchenProfileResponse;
import com.oyuki.kitchen.service.KitchenProfileService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
@RestController
@RequestMapping("/api/kitchen/profile")
public class KitchenProfileController {

    private final KitchenProfileService kitchenProfileService;

    public KitchenProfileController(
            KitchenProfileService kitchenProfileService
    ) {
        this.kitchenProfileService = kitchenProfileService;
    }

    @PutMapping
    public ApiResponse<KitchenProfileResponse> saveProfile(
            Authentication authentication,
            @Valid @RequestBody KitchenProfileRequest request
    ) {
        Long userId = (Long) authentication.getPrincipal();

        return ApiResponse.success(
                "Kitchen profile saved successfully",
                kitchenProfileService.saveProfile(userId, request)
        );
    }

    @GetMapping
    public ApiResponse<KitchenProfileResponse> getProfile(
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();

        return ApiResponse.success(
                "Kitchen profile retrieved successfully",
                kitchenProfileService.getProfile(userId)
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
    Long userId =
            (Long) authentication.getPrincipal();

    String imageUrl =
            kitchenProfileService.uploadProfileImage(
                    userId,
                    file
            );

    return ApiResponse.success(
            "Kitchen profile image uploaded successfully",
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
    Long userId =
            (Long) authentication.getPrincipal();

    String imageUrl =
            kitchenProfileService.uploadCoverImage(
                    userId,
                    file
            );

    return ApiResponse.success(
            "Kitchen cover image uploaded successfully",
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
    Long userId =
            (Long) authentication.getPrincipal();

    String documentUrl =
            kitchenProfileService.uploadIdDocument(
                    userId,
                    file
            );

    return ApiResponse.success(
            "Kitchen identification document uploaded successfully",
            Map.of("url", documentUrl)
    );
}
}