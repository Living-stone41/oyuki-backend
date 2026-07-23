package com.oyuki.marketplace.service;

import com.oyuki.product.dto.ProductResponse;
import com.oyuki.product.entity.Product;
import com.oyuki.product.enums.ProductStatus;
import com.oyuki.product.enums.ProductType;
import com.oyuki.product.repository.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Stream;

@Service
public class MarketplaceService {

    private final ProductRepository productRepository;

    public MarketplaceService(
            ProductRepository productRepository
    ) {
        this.productRepository = productRepository;
    }

    /*
     * VIEW AND FILTER ALL ACTIVE PRODUCTS
     */
    @Transactional(readOnly = true)
    public List<ProductResponse> getProducts(
            String search,
            String state,
            String lga,
            String category,
            ProductType productType
    ) {
        List<Product> activeProducts =
                productRepository
                        .findAllByStatusOrderByCreatedAtDesc(
                                ProductStatus.ACTIVE
                        );

        Stream<Product> productStream =
                activeProducts.stream();

        if (hasText(search)) {
            String searchValue = search.trim().toLowerCase();

            productStream = productStream.filter(product ->
                    containsIgnoreCase(
                            product.getName(),
                            searchValue
                    )
                    ||
                    containsIgnoreCase(
                            product.getDescription(),
                            searchValue
                    )
                    ||
                    containsIgnoreCase(
                            product.getCategory(),
                            searchValue
                    )
            );
        }

        if (hasText(state)) {
            productStream = productStream.filter(product ->
                    equalsIgnoreCase(
                            product.getState(),
                            state
                    )
            );
        }

        if (hasText(lga)) {
            productStream = productStream.filter(product ->
                    equalsIgnoreCase(
                            product.getLga(),
                            lga
                    )
            );
        }

        if (hasText(category)) {
            productStream = productStream.filter(product ->
                    equalsIgnoreCase(
                            product.getCategory(),
                            category
                    )
            );
        }

        if (productType != null) {
            productStream = productStream.filter(product ->
                    product.getProductType() == productType
            );
        }

        return productStream
                .map(ProductResponse::from)
                .toList();
    }

    /*
     * VIEW ONE ACTIVE PRODUCT
     */
    @Transactional
    public ProductResponse getProduct(Long productId) {
        Product product = productRepository
                .findByIdAndStatus(
                        productId,
                        ProductStatus.ACTIVE
                )
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Product not found"
                        )
                );

        product.setViewCount(
                product.getViewCount() + 1
        );

        Product savedProduct =
                productRepository.save(product);

        return ProductResponse.from(savedProduct);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private boolean equalsIgnoreCase(
            String actualValue,
            String requestedValue
    ) {
        return actualValue != null
                && actualValue.equalsIgnoreCase(
                        requestedValue.trim()
                );
    }

    private boolean containsIgnoreCase(
            String actualValue,
            String searchValue
    ) {
        return actualValue != null
                && actualValue
                .toLowerCase()
                .contains(searchValue);
    }
}