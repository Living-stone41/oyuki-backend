package com.oyuki.cart.service;

import com.oyuki.cart.dto.AddCartItemRequest;
import com.oyuki.cart.dto.CartResponse;
import com.oyuki.cart.dto.UpdateCartItemRequest;
import com.oyuki.cart.entity.Cart;
import com.oyuki.cart.entity.CartItem;
import com.oyuki.cart.repository.CartItemRepository;
import com.oyuki.cart.repository.CartRepository;
import com.oyuki.product.entity.Product;
import com.oyuki.product.entity.ProductVariant;
import com.oyuki.product.enums.ProductStatus;
import com.oyuki.product.repository.ProductVariantRepository;
import com.oyuki.user.entity.User;
import com.oyuki.user.enums.AccountStatus;
import com.oyuki.user.enums.Role;
import com.oyuki.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository productVariantRepository;
    private final UserRepository userRepository;

    public CartService(
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            ProductVariantRepository productVariantRepository,
            UserRepository userRepository
    ) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productVariantRepository = productVariantRepository;
        this.userRepository = userRepository;
    }

    /*
     * VIEW CUSTOMER CART
     */
    @Transactional
    public CartResponse getCart(Long customerId) {
        User customer = getActiveCustomer(customerId);

        Cart cart = getOrCreateCart(customer);

        return CartResponse.from(cart);
    }

    /*
     * ADD ITEM TO CART
     */
    @Transactional
    public CartResponse addItem(
            Long customerId,
            AddCartItemRequest request
    ) {
        User customer = getActiveCustomer(customerId);

        Cart cart = getOrCreateCart(customer);

        ProductVariant variant = productVariantRepository
                .findById(request.variantId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Product variant not found"
                ));

        validateVariant(variant);

        CartItem existingItem = cartItemRepository
                .findByCart_IdAndVariant_Id(
                        cart.getId(),
                        variant.getId()
                )
                .orElse(null);

        int newQuantity;

        if (existingItem != null) {
            newQuantity =
                    existingItem.getQuantity() +
                    request.quantity();

            validateQuantity(variant, newQuantity);

            existingItem.setQuantity(newQuantity);

            cartItemRepository.save(existingItem);
        }
        else {
            newQuantity = request.quantity();

            validateQuantity(variant, newQuantity);

            CartItem cartItem = CartItem.builder()
                    .variant(variant)
                    .quantity(newQuantity)
                    .build();

            cart.addItem(cartItem);

            cartRepository.save(cart);
        }

        return CartResponse.from(cart);
    }

    /*
     * UPDATE CART ITEM QUANTITY
     */
    @Transactional
    public CartResponse updateItem(
            Long customerId,
            Long cartItemId,
            UpdateCartItemRequest request
    ) {
        getActiveCustomer(customerId);

        CartItem cartItem = getCustomerCartItem(
                customerId,
                cartItemId
        );

        ProductVariant variant = cartItem.getVariant();

        validateVariant(variant);
        validateQuantity(variant, request.quantity());

        cartItem.setQuantity(request.quantity());

        cartItemRepository.save(cartItem);

        return CartResponse.from(cartItem.getCart());
    }

    /*
     * REMOVE ONE ITEM
     */
    @Transactional
    public CartResponse removeItem(
            Long customerId,
            Long cartItemId
    ) {
        getActiveCustomer(customerId);

        CartItem cartItem = getCustomerCartItem(
                customerId,
                cartItemId
        );

        Cart cart = cartItem.getCart();

        cart.removeItem(cartItem);

        cartRepository.save(cart);

        return CartResponse.from(cart);
    }

    /*
     * CLEAR ENTIRE CART
     */
    @Transactional
    public CartResponse clearCart(Long customerId) {
        User customer = getActiveCustomer(customerId);

        Cart cart = getOrCreateCart(customer);

        cart.clearItems();

        cartRepository.save(cart);

        return CartResponse.from(cart);
    }

    private Cart getOrCreateCart(User customer) {
        return cartRepository
                .findByCustomer_Id(customer.getId())
                .orElseGet(() -> {
                    Cart cart = Cart.builder()
                            .customer(customer)
                            .build();

                    return cartRepository.save(cart);
                });
    }

    private CartItem getCustomerCartItem(
            Long customerId,
            Long cartItemId
    ) {
        return cartItemRepository
                .findByIdAndCart_Customer_Id(
                        cartItemId,
                        customerId
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Cart item not found"
                ));
    }

    private User getActiveCustomer(Long customerId) {
        User customer = userRepository
                .findById(customerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Customer account not found"
                ));

        if (customer.getRole() != Role.CUSTOMER) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only customers can use the shopping cart"
            );
        }

        if (customer.getStatus() != AccountStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Your customer account is not active"
            );
        }

        return customer;
    }

    private void validateVariant(ProductVariant variant) {
        Product product = variant.getProduct();

        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "This product is not currently available"
            );
        }

        if (
                product.getOwner().getStatus()
                        != AccountStatus.ACTIVE
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The seller or kitchen is not currently available"
            );
        }

        if (!variant.isAvailable()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "This product option is not available"
            );
        }

        if (variant.getStockQuantity() < 1) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "This product option is out of stock"
            );
        }
    }

    private void validateQuantity(
            ProductVariant variant,
            int quantity
    ) {
        if (quantity < variant.getMinimumOrderQuantity()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Minimum order quantity is " +
                            variant.getMinimumOrderQuantity()
            );
        }

        if (quantity > variant.getStockQuantity()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only " +
                            variant.getStockQuantity() +
                            " units are available"
            );
        }
    }
}