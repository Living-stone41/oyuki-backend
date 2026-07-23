package com.oyuki.wishlist.service;

import com.oyuki.product.entity.Product;
import com.oyuki.product.enums.ProductStatus;
import com.oyuki.product.repository.ProductRepository;
import com.oyuki.user.entity.User;
import com.oyuki.user.enums.AccountStatus;
import com.oyuki.user.enums.Role;
import com.oyuki.user.repository.UserRepository;
import com.oyuki.wishlist.dto.WishlistCheckResponse;
import com.oyuki.wishlist.dto.WishlistResponse;
import com.oyuki.wishlist.entity.WishlistItem;
import com.oyuki.wishlist.repository.WishlistRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public WishlistService(
            WishlistRepository wishlistRepository,
            ProductRepository productRepository,
            UserRepository userRepository
    ) {
        this.wishlistRepository =
                wishlistRepository;

        this.productRepository =
                productRepository;

        this.userRepository =
                userRepository;
    }

    /*
     * =========================================================
     * ADD PRODUCT TO WISHLIST
     * =========================================================
     */

    @Transactional
    public WishlistResponse addProduct(
            Long customerId,
            Long productId
    ) {
        User customer =
                getActiveCustomer(customerId);

        Product product =
                getProduct(productId);

        validateProductCanBeSaved(product);

        Optional<WishlistItem> existingItem =
                wishlistRepository
                        .findByCustomer_IdAndProduct_Id(
                                customerId,
                                productId
                        );

        /*
         * The endpoint is idempotent.
         *
         * Saving an already-saved product simply
         * returns the existing wishlist item.
         */
        if (existingItem.isPresent()) {
            return WishlistResponse.from(
                    existingItem.get()
            );
        }

        WishlistItem wishlistItem =
                WishlistItem.builder()
                        .customer(customer)
                        .product(product)
                        .build();

        return WishlistResponse.from(
                wishlistRepository.save(
                        wishlistItem
                )
        );
    }

    /*
     * =========================================================
     * GET CUSTOMER WISHLIST
     * =========================================================
     */

    @Transactional
    public List<WishlistResponse> getWishlist(
            Long customerId
    ) {
        getActiveCustomer(customerId);

        List<WishlistItem> wishlistItems =
                wishlistRepository
                        .findAllByCustomer_IdOrderByCreatedAtDesc(
                                customerId
                        );

        /*
         * Remove products that should no longer
         * appear in a customer's wishlist.
         */
        List<WishlistItem> unavailableItems =
                wishlistItems.stream()
                        .filter(item ->
                                !isDisplayable(
                                        item.getProduct()
                                )
                        )
                        .toList();

        if (!unavailableItems.isEmpty()) {
            wishlistRepository.deleteAll(
                    unavailableItems
            );
        }

        return wishlistItems.stream()
                .filter(item ->
                        isDisplayable(
                                item.getProduct()
                        )
                )
                .map(WishlistResponse::from)
                .toList();
    }

    /*
     * =========================================================
     * CHECK PRODUCT
     * =========================================================
     */

    @Transactional(readOnly = true)
    public WishlistCheckResponse checkProduct(
            Long customerId,
            Long productId
    ) {
        getActiveCustomer(customerId);

        /*
         * Confirm the product exists before checking.
         */
        getProduct(productId);

        boolean saved =
                wishlistRepository
                        .existsByCustomer_IdAndProduct_Id(
                                customerId,
                                productId
                        );

        return new WishlistCheckResponse(
                productId,
                saved
        );
    }

    /*
     * =========================================================
     * REMOVE ONE PRODUCT
     * =========================================================
     */

    @Transactional
    public void removeProduct(
            Long customerId,
            Long productId
    ) {
        getActiveCustomer(customerId);

        /*
         * The operation is safe even if the item
         * has already been removed.
         */
        wishlistRepository
                .deleteByCustomer_IdAndProduct_Id(
                        customerId,
                        productId
                );
    }

    /*
     * =========================================================
     * CLEAR WISHLIST
     * =========================================================
     */

    @Transactional
    public long clearWishlist(
            Long customerId
    ) {
        getActiveCustomer(customerId);

        return wishlistRepository
                .deleteAllByCustomer_Id(
                        customerId
                );
    }

    /*
     * =========================================================
     * COUNT CUSTOMER WISHLIST ITEMS
     * =========================================================
     */

    @Transactional(readOnly = true)
    public long countWishlistItems(
            Long customerId
    ) {
        getActiveCustomer(customerId);

        return wishlistRepository
                .countByCustomer_Id(
                        customerId
                );
    }

    /*
     * =========================================================
     * VALIDATION
     * =========================================================
     */

    private User getActiveCustomer(
            Long customerId
    ) {
        User customer =
                userRepository
                        .findById(customerId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Customer account not found"
                                )
                        );

        if (
                customer.getRole()
                        != Role.CUSTOMER
        ) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only customers can manage a wishlist"
            );
        }

        if (
                customer.getStatus()
                        != AccountStatus.ACTIVE
        ) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Customer account is not active"
            );
        }

        return customer;
    }

    private Product getProduct(
            Long productId
    ) {
        return productRepository
                .findById(productId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Product not found"
                        )
                );
    }

    private void validateProductCanBeSaved(
            Product product
    ) {
        if (!isDisplayable(product)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "This product is no longer available"
            );
        }
    }

    private boolean isDisplayable(
            Product product
    ) {
        if (
                product == null ||
                product.getStatus() == null
        ) {
            return false;
        }

        /*
         * Out-of-stock products remain visible
         * because customers may want to save them
         * until they are restocked.
         */
        return product.getStatus()
                        == ProductStatus.ACTIVE ||
                product.getStatus()
                        == ProductStatus.OUT_OF_STOCK;
    }
}