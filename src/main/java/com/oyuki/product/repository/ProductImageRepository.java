package com.oyuki.product.repository;

import com.oyuki.product.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductImageRepository
        extends JpaRepository<ProductImage, Long> {

    List<ProductImage>
    findAllByProduct_IdOrderByDisplayOrderAsc(
            Long productId
    );

    Optional<ProductImage> findByIdAndProduct_Id(
            Long imageId,
            Long productId
    );

    long countByProduct_Id(Long productId);
}