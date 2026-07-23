package com.oyuki.marketplace.controller;

import com.oyuki.marketplace.service.MarketplaceService;
import com.oyuki.product.dto.ProductResponse;
import com.oyuki.product.enums.ProductType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/marketplace/products")
public class MarketplaceController {

    private final MarketplaceService marketplaceService;

    public MarketplaceController(
            MarketplaceService marketplaceService
    ) {
        this.marketplaceService = marketplaceService;
    }

    /*
     * VIEW AND FILTER MARKETPLACE PRODUCTS
     */
    @GetMapping
    public ResponseEntity<List<ProductResponse>> getProducts(

            @RequestParam(
                    required = false
            ) String search,

            @RequestParam(
                    required = false
            ) String state,

            @RequestParam(
                    required = false
            ) String lga,

            @RequestParam(
                    required = false
            ) String category,

            @RequestParam(
                    required = false
            ) ProductType productType
    ) {
        return ResponseEntity.ok(
                marketplaceService.getProducts(
                        search,
                        state,
                        lga,
                        category,
                        productType
                )
        );
    }

    /*
     * VIEW ONE PRODUCT
     */
    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponse> getProduct(
            @PathVariable Long productId
    ) {
        return ResponseEntity.ok(
                marketplaceService.getProduct(productId)
        );
    }
}