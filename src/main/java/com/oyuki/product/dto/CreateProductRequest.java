package com.oyuki.product.dto;

import com.oyuki.product.enums.ProductType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateProductRequest(

        @NotBlank(message = "Product name is required")
        @Size(
                max = 150,
                message = "Product name cannot exceed 150 characters"
        )
        String name,

        @NotBlank(message = "Product description is required")
        @Size(
                min = 10,
                max = 2000,
                message = "Description must contain between 10 and 2000 characters"
        )
        String description,

        @NotBlank(message = "Product category is required")
        @Size(
                max = 100,
                message = "Category cannot exceed 100 characters"
        )
        String category,

        @NotNull(message = "Product type is required")
        ProductType productType,

        @NotBlank(message = "State is required")
        String state,

        @NotBlank(message = "LGA is required")
        String lga,

        @NotBlank(message = "Area is required")
        String area,

        @Valid
        @NotEmpty(
                message = "Add at least one measurement and price option"
        )
        List<ProductVariantRequest> variants
) {
}