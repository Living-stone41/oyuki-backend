package com.oyuki.product.controller;

import com.oyuki.product.dto.CreateProductRequest;
import com.oyuki.product.dto.ProductResponse;
import com.oyuki.product.dto.ProductVariantRequest;
import com.oyuki.product.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(
            ProductService productService
    ) {
        this.productService = productService;
    }

    /*
     * CREATE PRODUCT
     */
    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(
            Authentication authentication,
            @Valid @RequestBody CreateProductRequest request
    ) {
        Long userId = getUserId(authentication);

        ProductResponse response =
                productService.createProduct(userId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /*
     * GET ALL PRODUCTS BELONGING TO LOGGED-IN USER
     */
    @GetMapping
    public ResponseEntity<List<ProductResponse>> getMyProducts(
            Authentication authentication
    ) {
        Long userId = getUserId(authentication);

        return ResponseEntity.ok(
                productService.getMyProducts(userId)
        );
    }

    /*
     * GET ONE PRODUCT
     */
    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponse> getMyProduct(
            Authentication authentication,
            @PathVariable Long productId
    ) {
        Long userId = getUserId(authentication);

        return ResponseEntity.ok(
                productService.getMyProduct(
                        userId,
                        productId
                )
        );
    }

    /*
     * UPDATE PRODUCT
     */
    @PutMapping("/{productId}")
    public ResponseEntity<ProductResponse> updateProduct(
            Authentication authentication,
            @PathVariable Long productId,
            @Valid @RequestBody CreateProductRequest request
    ) {
        Long userId = getUserId(authentication);

        return ResponseEntity.ok(
                productService.updateProduct(
                        userId,
                        productId,
                        request
                )
        );
    }

    /*
     * DELETE PRODUCT
     */
    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deleteProduct(
            Authentication authentication,
            @PathVariable Long productId
    ) {
        Long userId = getUserId(authentication);

        productService.deleteProduct(
                userId,
                productId
        );

        return ResponseEntity.noContent().build();
    }

    /*
     * UPLOAD PRODUCT IMAGE
     */
    @PostMapping(
            value = "/{productId}/images",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ProductResponse> uploadImage(
            Authentication authentication,
            @PathVariable Long productId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(
                    value = "primary",
                    defaultValue = "false"
            ) boolean primary
    ) {
        Long userId = getUserId(authentication);

        return ResponseEntity.ok(
                productService.uploadImage(
                        userId,
                        productId,
                        file,
                        primary
                )
        );
    }

    /*
     * DELETE PRODUCT IMAGE
     */
    @DeleteMapping(
            "/{productId}/images/{imageId}"
    )
    public ResponseEntity<ProductResponse> deleteImage(
            Authentication authentication,
            @PathVariable Long productId,
            @PathVariable Long imageId
    ) {
        Long userId = getUserId(authentication);

        return ResponseEntity.ok(
                productService.deleteImage(
                        userId,
                        productId,
                        imageId
                )
        );
    }

    /*
     * ADD PRODUCT VARIANT
     */
    @PostMapping(
            "/{productId}/variants"
    )
    public ResponseEntity<ProductResponse> addVariant(
            Authentication authentication,
            @PathVariable Long productId,
            @Valid @RequestBody ProductVariantRequest request
    ) {
        Long userId = getUserId(authentication);

        ProductResponse response =
                productService.addVariant(
                        userId,
                        productId,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /*
     * UPDATE PRODUCT VARIANT
     */
    @PutMapping(
            "/{productId}/variants/{variantId}"
    )
    public ResponseEntity<ProductResponse> updateVariant(
            Authentication authentication,
            @PathVariable Long productId,
            @PathVariable Long variantId,
            @Valid @RequestBody ProductVariantRequest request
    ) {
        Long userId = getUserId(authentication);

        return ResponseEntity.ok(
                productService.updateVariant(
                        userId,
                        productId,
                        variantId,
                        request
                )
        );
    }

    /*
     * DELETE PRODUCT VARIANT
     */
    @DeleteMapping(
            "/{productId}/variants/{variantId}"
    )
    public ResponseEntity<ProductResponse> deleteVariant(
            Authentication authentication,
            @PathVariable Long productId,
            @PathVariable Long variantId
    ) {
        Long userId = getUserId(authentication);

        return ResponseEntity.ok(
                productService.deleteVariant(
                        userId,
                        productId,
                        variantId
                )
        );
    }

    private Long getUserId(
            Authentication authentication
    ) {
        return (Long) authentication.getPrincipal();
    }
}