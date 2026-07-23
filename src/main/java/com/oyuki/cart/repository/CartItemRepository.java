package com.oyuki.cart.repository;

import com.oyuki.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository
        extends JpaRepository<CartItem, Long> {

    List<CartItem> findAllByCart_IdOrderByCreatedAtDesc(
            Long cartId
    );

    Optional<CartItem> findByCart_IdAndVariant_Id(
            Long cartId,
            Long variantId
    );

    Optional<CartItem> findByIdAndCart_Customer_Id(
            Long cartItemId,
            Long customerId
    );

    void deleteAllByCart_Id(Long cartId);
}