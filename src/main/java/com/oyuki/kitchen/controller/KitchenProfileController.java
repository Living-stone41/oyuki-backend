package com.oyuki.kitchen.controller;

import com.oyuki.common.response.ApiResponse;
import com.oyuki.kitchen.dto.*;
import com.oyuki.kitchen.service.KitchenProfileService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/kitchen/profile")
public class KitchenProfileController {
    private final KitchenProfileService kitchenProfileService;
    public KitchenProfileController(KitchenProfileService kitchenProfileService) { this.kitchenProfileService = kitchenProfileService; }
    private Long userId(Authentication a) { return (Long) a.getPrincipal(); }

    @PutMapping public ApiResponse<KitchenProfileResponse> saveProfile(Authentication a, @Valid @RequestBody KitchenProfileRequest r) {
        return ApiResponse.success("Kitchen profile saved successfully", kitchenProfileService.saveProfile(userId(a), r));
    }
    @GetMapping public ApiResponse<KitchenProfileResponse> getProfile(Authentication a) {
        return ApiResponse.success("Kitchen profile retrieved successfully", kitchenProfileService.getProfile(userId(a)));
    }
    @PostMapping(value="/profile-image", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Map<String,String>> uploadProfileImage(Authentication a,@RequestParam("file") MultipartFile f){return ApiResponse.success("Kitchen profile image uploaded successfully",Map.of("url",kitchenProfileService.uploadProfileImage(userId(a),f)));}
    @PostMapping(value="/cover-image", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Map<String,String>> uploadCoverImage(Authentication a,@RequestParam("file") MultipartFile f){return ApiResponse.success("Kitchen cover image uploaded successfully",Map.of("url",kitchenProfileService.uploadCoverImage(userId(a),f)));}
    @PostMapping(value="/id-document", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Map<String,String>> uploadIdDocument(Authentication a,@RequestParam("file") MultipartFile f){return ApiResponse.success("Kitchen identification document uploaded successfully",Map.of("url",kitchenProfileService.uploadIdDocument(userId(a),f)));}
    @PostMapping(value="/gallery", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<KitchenImageResponse> uploadKitchenImage(Authentication a,@RequestParam("file") MultipartFile f,@RequestParam(value="caption",required=false) String c){return ApiResponse.success("Kitchen picture uploaded successfully",kitchenProfileService.uploadKitchenImage(userId(a),f,c));}
    @GetMapping("/gallery") public ApiResponse<List<KitchenImageResponse>> gallery(Authentication a){return ApiResponse.success("Kitchen pictures retrieved successfully",kitchenProfileService.getKitchenImages(userId(a)));}
    @DeleteMapping("/gallery/{imageId}") public ApiResponse<Void> delete(Authentication a,@PathVariable Long imageId){kitchenProfileService.deleteKitchenImage(userId(a),imageId);return ApiResponse.success("Kitchen picture deleted successfully",null);}
}
