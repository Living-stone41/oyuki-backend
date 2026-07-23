package com.oyuki.product.repository;

import com.oyuki.product.entity.Product;
import com.oyuki.product.enums.ProductStatus;
import com.oyuki.product.enums.ProductType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository
        extends JpaRepository<Product, Long> {

    /*
     * Seller or kitchen dashboard:
     * get all products belonging to the logged-in owner.
     */
    List<Product> findAllByOwner_IdAndStatusNotOrderByCreatedAtDesc(
            Long ownerId,
            ProductStatus excludedStatus
    );

    /*
     * Find one product belonging to a particular owner.
     */
    Optional<Product> findByIdAndOwner_Id(
            Long productId,
            Long ownerId
    );

    /*
     * Marketplace: get all products with a given status.
     */
    List<Product> findAllByStatusOrderByCreatedAtDesc(
            ProductStatus status
    );

    /*
     * Marketplace: get one active product.
     */
    Optional<Product> findByIdAndStatus(
            Long productId,
            ProductStatus status
    );

    List<Product> findAllByStatusAndProductTypeOrderByCreatedAtDesc(
            ProductStatus status,
            ProductType productType
    );

    List<Product> findAllByStatusAndStateIgnoreCaseOrderByCreatedAtDesc(
            ProductStatus status,
            String state
    );

    List<Product> findAllByStatusAndLgaIgnoreCaseOrderByCreatedAtDesc(
            ProductStatus status,
            String lga
    );

    List<Product> findAllByStatusAndCategoryIgnoreCaseOrderByCreatedAtDesc(
            ProductStatus status,
            String category
    );

    List<Product> findAllByStatusAndNameContainingIgnoreCaseOrderByCreatedAtDesc(
            ProductStatus status,
            String name
    );
}