package com.oyuki.wishlist.repository;

import com.oyuki.wishlist.entity.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistRepository
        extends JpaRepository<WishlistItem, Long> {

    List<WishlistItem>
    findAllByCustomer_IdOrderByCreatedAtDesc(
            Long customerId
    );

    Optional<WishlistItem>
    findByCustomer_IdAndProduct_Id(
            Long customerId,
            Long productId
    );

    boolean existsByCustomer_IdAndProduct_Id(
            Long customerId,
            Long productId
    );

    long deleteByCustomer_IdAndProduct_Id(
            Long customerId,
            Long productId
    );

    long deleteAllByCustomer_Id(
            Long customerId
    );

    /*
     * Can be used by ProductService when a product
     * is permanently deleted.
     */
    long deleteAllByProduct_Id(
            Long productId
    );

    long countByCustomer_Id(
            Long customerId
    );
}