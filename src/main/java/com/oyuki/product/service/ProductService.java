package com.oyuki.product.service;

import com.oyuki.common.storage.FileStorageService;
import com.oyuki.product.dto.CreateProductRequest;
import com.oyuki.product.dto.ProductResponse;
import com.oyuki.product.dto.ProductVariantRequest;
import com.oyuki.product.entity.Product;
import com.oyuki.product.entity.ProductImage;
import com.oyuki.product.entity.ProductVariant;
import com.oyuki.product.enums.ProductStatus;
import com.oyuki.product.enums.ProductType;
import com.oyuki.product.repository.ProductImageRepository;
import com.oyuki.product.repository.ProductRepository;
import com.oyuki.product.repository.ProductVariantRepository;
import com.oyuki.user.entity.User;
import com.oyuki.user.enums.AccountStatus;
import com.oyuki.user.enums.Role;
import com.oyuki.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductImageRepository productImageRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    public ProductService(
            ProductRepository productRepository,
            ProductVariantRepository productVariantRepository,
            ProductImageRepository productImageRepository,
            UserRepository userRepository,
            FileStorageService fileStorageService
    ) {
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.productImageRepository = productImageRepository;
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
    }

    /*
     * CREATE PRODUCT
     */
    @Transactional
    public ProductResponse createProduct(
            Long ownerId,
            CreateProductRequest request
    ) {
        User owner = getActiveProductOwner(ownerId);

        validateProductType(owner, request.productType());

        Product product = Product.builder()
                .owner(owner)
                .name(clean(request.name()))
                .description(clean(request.description()))
                .category(clean(request.category()))
                .productType(request.productType())
                .state(clean(request.state()))
                .lga(clean(request.lga()))
                .area(clean(request.area()))
                .status(ProductStatus.DRAFT)
                .build();

        for (ProductVariantRequest variantRequest : request.variants()) {
            ProductVariant variant = createVariant(variantRequest);
            product.addVariant(variant);
        }

        product.setStatus(calculateStockStatus(product.getVariants()));

        Product savedProduct = productRepository.save(product);

        return ProductResponse.from(savedProduct);
    }

    /*
     * GET ALL PRODUCTS BELONGING TO LOGGED-IN OWNER
     */
    @Transactional(readOnly = true)
    public List<ProductResponse> getMyProducts(Long ownerId) {
        getProductOwner(ownerId);

        return productRepository
                .findAllByOwner_IdAndStatusNotOrderByCreatedAtDesc(
                        ownerId,
                        ProductStatus.DELETED
                )
                .stream()
                .map(ProductResponse::from)
                .toList();
    }

    /*
     * GET ONE PRODUCT
     */
    @Transactional(readOnly = true)
    public ProductResponse getMyProduct(
            Long ownerId,
            Long productId
    ) {
        Product product = getOwnedProduct(ownerId, productId);

        return ProductResponse.from(product);
    }

    /*
     * UPDATE PRODUCT
     */
    @Transactional
    public ProductResponse updateProduct(
            Long ownerId,
            Long productId,
            CreateProductRequest request
    ) {
        User owner = getActiveProductOwner(ownerId);
        Product product = getOwnedProduct(ownerId, productId);

        validateProductType(owner, request.productType());

        product.setName(clean(request.name()));
        product.setDescription(clean(request.description()));
        product.setCategory(clean(request.category()));
        product.setProductType(request.productType());
        product.setState(clean(request.state()));
        product.setLga(clean(request.lga()));
        product.setArea(clean(request.area()));

        /*
         * Replace the existing variants with the variants
         * submitted during the product update.
         */
        product.getVariants().clear();

        for (ProductVariantRequest variantRequest : request.variants()) {
            ProductVariant variant = createVariant(variantRequest);
            product.addVariant(variant);
        }

        product.setStatus(calculateStockStatus(product.getVariants()));

        Product savedProduct = productRepository.save(product);

        return ProductResponse.from(savedProduct);
    }

    /*
     * SOFT DELETE PRODUCT
     */
    @Transactional
    public void deleteProduct(
            Long ownerId,
            Long productId
    ) {
        getActiveProductOwner(ownerId);

        Product product = getOwnedProduct(ownerId, productId);

        product.setStatus(ProductStatus.DELETED);

        productRepository.save(product);
    }

    /*
     * ADD PRODUCT VARIANT
     *
     * Example:
     * 1 KILOGRAM - ₦1,800
     */
    @Transactional
    public ProductResponse addVariant(
            Long ownerId,
            Long productId,
            ProductVariantRequest request
    ) {
        getActiveProductOwner(ownerId);

        Product product = getOwnedProduct(ownerId, productId);

        ProductVariant variant = createVariant(request);

        product.addVariant(variant);
        product.setStatus(calculateStockStatus(product.getVariants()));

        Product savedProduct = productRepository.save(product);

        return ProductResponse.from(savedProduct);
    }

    /*
     * UPDATE PRODUCT VARIANT
     */
    @Transactional
    public ProductResponse updateVariant(
            Long ownerId,
            Long productId,
            Long variantId,
            ProductVariantRequest request
    ) {
        getActiveProductOwner(ownerId);

        Product product = getOwnedProduct(ownerId, productId);

        ProductVariant variant = productVariantRepository
                .findByIdAndProduct_Id(variantId, productId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Product variant not found"
                ));

        updateVariantFields(variant, request);

        productVariantRepository.save(variant);

        product.setStatus(calculateStockStatus(product.getVariants()));
        productRepository.save(product);

        return ProductResponse.from(product);
    }

    /*
     * DELETE PRODUCT VARIANT
     */
    @Transactional
    public ProductResponse deleteVariant(
            Long ownerId,
            Long productId,
            Long variantId
    ) {
        getActiveProductOwner(ownerId);

        Product product = getOwnedProduct(ownerId, productId);

        ProductVariant variant = productVariantRepository
                .findByIdAndProduct_Id(variantId, productId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Product variant not found"
                ));

        if (product.getVariants().size() <= 1) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A product must have at least one variant"
            );
        }

        product.removeVariant(variant);
        product.setStatus(calculateStockStatus(product.getVariants()));

        Product savedProduct = productRepository.save(product);

        return ProductResponse.from(savedProduct);
    }

    /*
     * UPLOAD PRODUCT IMAGE
     */
    @Transactional
    public ProductResponse uploadImage(
            Long ownerId,
            Long productId,
            MultipartFile file,
            boolean primary
    ) {
        getActiveProductOwner(ownerId);

        Product product = getOwnedProduct(ownerId, productId);

        long imageCount =
                productImageRepository.countByProduct_Id(productId);

        if (imageCount >= 5) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A product can have a maximum of five images"
            );
        }

        String imageUrl =
                fileStorageService.storeImage(file, "products");

        boolean shouldBePrimary = primary || imageCount == 0;

        if (shouldBePrimary) {
            for (ProductImage existingImage : product.getImages()) {
                existingImage.setPrimaryImage(false);
            }
        }

        ProductImage image = ProductImage.builder()
                .imageUrl(imageUrl)
                .primaryImage(shouldBePrimary)
                .displayOrder((int) imageCount)
                .build();

        product.addImage(image);

        Product savedProduct = productRepository.save(product);

        return ProductResponse.from(savedProduct);
    }

    /*
     * DELETE PRODUCT IMAGE
     */
    @Transactional
    public ProductResponse deleteImage(
            Long ownerId,
            Long productId,
            Long imageId
    ) {
        getActiveProductOwner(ownerId);

        Product product = getOwnedProduct(ownerId, productId);

        ProductImage image = productImageRepository
                .findByIdAndProduct_Id(imageId, productId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Product image not found"
                ));

        boolean deletedImageWasPrimary = image.isPrimaryImage();

        product.removeImage(image);

        if (
                deletedImageWasPrimary &&
                !product.getImages().isEmpty()
        ) {
            product.getImages().get(0).setPrimaryImage(true);
        }

        Product savedProduct = productRepository.save(product);

        return ProductResponse.from(savedProduct);
    }

    private ProductVariant createVariant(
            ProductVariantRequest request
    ) {
        ProductVariant variant = new ProductVariant();

        updateVariantFields(variant, request);

        return variant;
    }

    private void updateVariantFields(
            ProductVariant variant,
            ProductVariantRequest request
    ) {
        variant.setMeasurementValue(request.measurementValue());
        variant.setMeasurementUnit(request.measurementUnit());
        variant.setPrice(request.price());
        variant.setStockQuantity(request.stockQuantity());
        variant.setMinimumOrderQuantity(
                request.minimumOrderQuantity()
        );

        variant.setSku(
                request.sku() == null ||
                request.sku().isBlank()
                        ? null
                        : request.sku().trim()
        );

        variant.setAvailable(
                request.available() == null ||
                request.available()
        );
    }

    private ProductStatus calculateStockStatus(
            List<ProductVariant> variants
    ) {
        boolean hasAvailableStock = variants.stream()
                .anyMatch(variant ->
                        variant.isAvailable() &&
                        variant.getStockQuantity() > 0
                );

        if (hasAvailableStock) {
            return ProductStatus.PENDING_APPROVAL;
        }

        return ProductStatus.OUT_OF_STOCK;
    }

    private Product getOwnedProduct(
            Long ownerId,
            Long productId
    ) {
        Product product = productRepository
                .findByIdAndOwner_Id(productId, ownerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Product not found"
                ));

        if (product.getStatus() == ProductStatus.DELETED) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Product not found"
            );
        }

        return product;
    }

    private User getProductOwner(Long ownerId) {
        User user = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User account not found"
                ));

        if (
                user.getRole() != Role.SELLER &&
                user.getRole() != Role.KITCHEN
        ) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only sellers and kitchens can manage products"
            );
        }

        return user;
    }

    private User getActiveProductOwner(Long ownerId) {
        User user = getProductOwner(ownerId);

        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Your account must be approved before managing products"
            );
        }

        return user;
    }

    private void validateProductType(
            User owner,
            ProductType productType
    ) {
        if (
                owner.getRole() == Role.SELLER &&
                productType != ProductType.FARM_PRODUCT
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A seller can only add farm products"
            );
        }

        if (
                owner.getRole() == Role.KITCHEN &&
                productType == ProductType.FARM_PRODUCT
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A kitchen can only add ready meals or cooking services"
            );
        }
    }

    private String clean(String value) {
        return value == null ? null : value.trim();
    }
}