package com.oyuki.product.repository;

import com.oyuki.product.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository
        extends JpaRepository<ProductVariant, Long> {

    List<ProductVariant> findAllByProduct_IdOrderByPriceAsc(
            Long productId
    );

    Optional<ProductVariant> findByIdAndProduct_Id(
            Long variantId,
            Long productId
    );
}