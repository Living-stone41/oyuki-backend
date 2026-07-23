package com.oyuki.coupon.service;

import com.oyuki.cart.entity.Cart;
import com.oyuki.cart.entity.CartItem;
import com.oyuki.cart.repository.CartRepository;
import com.oyuki.coupon.dto.*;
import com.oyuki.coupon.entity.Coupon;
import com.oyuki.coupon.entity.CouponUsage;
import com.oyuki.coupon.enums.DiscountType;
import com.oyuki.coupon.repository.CouponRepository;
import com.oyuki.coupon.repository.CouponUsageRepository;
import com.oyuki.delivery.dto.DeliveryFeeResponse;
import com.oyuki.delivery.service.DeliveryFeeService;
import com.oyuki.order.entity.Order;
import com.oyuki.order.enums.DeliveryType;
import com.oyuki.order.repository.OrderRepository;
import com.oyuki.product.entity.ProductVariant;
import com.oyuki.user.entity.User;
import com.oyuki.user.enums.AccountStatus;
import com.oyuki.user.enums.Role;
import com.oyuki.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class CouponService {

    private final CouponRepository couponRepository;
    private final CouponUsageRepository couponUsageRepository;
    private final CartRepository cartRepository;
    private final DeliveryFeeService deliveryFeeService;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    public CouponService(
            CouponRepository couponRepository,
            CouponUsageRepository couponUsageRepository,
            CartRepository cartRepository,
            DeliveryFeeService deliveryFeeService,
            UserRepository userRepository,
            OrderRepository orderRepository
    ) {
        this.couponRepository = couponRepository;
        this.couponUsageRepository = couponUsageRepository;
        this.cartRepository = cartRepository;
        this.deliveryFeeService = deliveryFeeService;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
    }

    /*
     * =========================================================
     * ADMIN CRUD
     * =========================================================
     */

    @Transactional
    public CouponResponse createCoupon(
            Long adminId,
            CreateCouponRequest request
    ) {
        User admin = getActiveAdmin(adminId);

        String code = normalizeCode(request.code());

        if (couponRepository.existsByCodeIgnoreCase(code)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A coupon with this code already exists"
            );
        }

        validateCouponValues(
                request.discountType(),
                request.discountValue(),
                request.maximumDiscountAmount(),
                request.minimumOrderAmount(),
                request.usageLimit(),
                request.perCustomerLimit(),
                request.startsAt(),
                request.expiresAt()
        );

        Coupon coupon = Coupon.builder()
                .code(code)
                .description(clean(request.description()))
                .discountType(request.discountType())
                .discountValue(
                        normalizedDiscountValue(
                                request.discountType(),
                                request.discountValue()
                        )
                )
                .maximumDiscountAmount(
                        normalizeMoney(
                                request.maximumDiscountAmount()
                        )
                )
                .minimumOrderAmount(
                        defaultMoney(
                                request.minimumOrderAmount()
                        )
                )
                .usageLimit(request.usageLimit())
                .perCustomerLimit(
                        request.perCustomerLimit()
                )
                .startsAt(request.startsAt())
                .expiresAt(request.expiresAt())
                .active(
                        request.active() == null
                                || request.active()
                )
                .createdBy(admin)
                .updatedBy(admin)
                .build();

        Coupon saved = couponRepository.save(coupon);

        return CouponResponse.from(saved, 0);
    }

    @Transactional(readOnly = true)
    public List<CouponResponse> getCoupons(
            Long adminId,
            Boolean active
    ) {
        getActiveAdmin(adminId);

        List<Coupon> coupons =
                active == null
                        ? couponRepository
                        .findAllByOrderByCreatedAtDesc()
                        : couponRepository
                        .findAllByActiveOrderByCreatedAtDesc(
                                active
                        );

        return coupons.stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CouponResponse getCoupon(
            Long adminId,
            Long couponId
    ) {
        getActiveAdmin(adminId);

        return toResponse(
                getCouponEntity(couponId)
        );
    }

    @Transactional
    public CouponResponse updateCoupon(
            Long adminId,
            Long couponId,
            UpdateCouponRequest request
    ) {
        User admin = getActiveAdmin(adminId);

        Coupon coupon = getCouponEntity(couponId);

        String code = normalizeCode(request.code());

        if (
                couponRepository
                        .existsByCodeIgnoreCaseAndIdNot(
                                code,
                                couponId
                        )
        ) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A coupon with this code already exists"
            );
        }

        validateCouponValues(
                request.discountType(),
                request.discountValue(),
                request.maximumDiscountAmount(),
                request.minimumOrderAmount(),
                request.usageLimit(),
                request.perCustomerLimit(),
                request.startsAt(),
                request.expiresAt()
        );

        coupon.setCode(code);
        coupon.setDescription(
                clean(request.description())
        );
        coupon.setDiscountType(
                request.discountType()
        );
        coupon.setDiscountValue(
                normalizedDiscountValue(
                        request.discountType(),
                        request.discountValue()
                )
        );
        coupon.setMaximumDiscountAmount(
                normalizeMoney(
                        request.maximumDiscountAmount()
                )
        );
        coupon.setMinimumOrderAmount(
                defaultMoney(
                        request.minimumOrderAmount()
                )
        );
        coupon.setUsageLimit(
                request.usageLimit()
        );
        coupon.setPerCustomerLimit(
                request.perCustomerLimit()
        );
        coupon.setStartsAt(
                request.startsAt()
        );
        coupon.setExpiresAt(
                request.expiresAt()
        );
        coupon.setActive(
                request.active() == null
                        ? coupon.isActive()
                        : request.active()
        );
        coupon.setUpdatedBy(admin);

        return toResponse(
                couponRepository.save(coupon)
        );
    }

    @Transactional
    public CouponResponse updateStatus(
            Long adminId,
            Long couponId,
            boolean active
    ) {
        User admin = getActiveAdmin(adminId);

        Coupon coupon = getCouponEntity(couponId);

        coupon.setActive(active);
        coupon.setUpdatedBy(admin);

        return toResponse(
                couponRepository.save(coupon)
        );
    }

    @Transactional
    public void deleteCoupon(
            Long adminId,
            Long couponId
    ) {
        getActiveAdmin(adminId);

        Coupon coupon = getCouponEntity(couponId);

        long usageCount =
                couponUsageRepository
                        .countByCoupon_Id(couponId);

        if (usageCount > 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Used coupons cannot be deleted. Disable the coupon instead."
            );
        }

        couponRepository.delete(coupon);
    }

    /*
     * =========================================================
     * CUSTOMER VALIDATION
     * =========================================================
     */

    @Transactional(readOnly = true)
    public CouponValidationResponse validateCoupon(
            Long customerId,
            ValidateCouponRequest request
    ) {
        getActiveCustomer(customerId);

        Coupon coupon =
                couponRepository
                        .findByCodeIgnoreCase(
                                normalizeCode(request.code())
                        )
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Coupon not found"
                                )
                        );

        BigDecimal subtotal =
                calculateCartSubtotal(customerId);

        DeliveryType deliveryType =
                request.deliveryType() == null
                        ? DeliveryType.CUSTOMER_ADDRESS
                        : request.deliveryType();

        DeliveryFeeResponse deliveryFeeResponse;

        if (
                deliveryType
                        == DeliveryType.CUSTOMER_ADDRESS
        ) {
            if (request.addressId() == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Delivery address is required"
                );
            }

            deliveryFeeResponse =
                    deliveryFeeService
                            .calculateCartDeliveryFee(
                                    customerId,
                                    request.addressId()
                            );

        } else {
            if (request.destinationKitchenId() == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Destination kitchen is required"
                );
            }

            deliveryFeeResponse =
                    deliveryFeeService
                            .calculateCartDeliveryFeeToKitchen(
                                    customerId,
                                    request.destinationKitchenId()
                            );
        }

        BigDecimal deliveryFee =
                defaultMoney(
                        deliveryFeeResponse
                                .totalDeliveryFee()
                );

        validateCouponAvailability(
                coupon,
                customerId,
                subtotal
        );

        BigDecimal discountAmount =
                calculateDiscount(
                        coupon,
                        subtotal,
                        deliveryFee
                );

        BigDecimal totalAfterDiscount =
                subtotal
                        .add(deliveryFee)
                        .subtract(discountAmount)
                        .max(BigDecimal.ZERO)
                        .setScale(
                                2,
                                RoundingMode.HALF_UP
                        );

        return new CouponValidationResponse(
                true,
                "Coupon applied successfully",

                coupon.getId(),
                coupon.getCode(),
                coupon.getDiscountType(),

                subtotal,
                deliveryFee,
                discountAmount,
                totalAfterDiscount,

                calculateRemainingGlobalUses(coupon),
                calculateRemainingCustomerUses(
                        coupon,
                        customerId
                )
        );
    }

    /*
     * Used later by checkout after the order has
     * been saved successfully.
     */
    @Transactional
    public CouponUsage recordUsage(
            Long customerId,
            Long orderId,
            String couponCode,
            BigDecimal discountAmount
    ) {
        User customer = getActiveCustomer(customerId);

        Order order =
                orderRepository
                        .findByIdAndCustomer_Id(
                                orderId,
                                customerId
                        )
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Order not found"
                                )
                        );

        Coupon coupon =
                couponRepository
                        .findByCodeIgnoreCase(
                                normalizeCode(couponCode)
                        )
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Coupon not found"
                                )
                        );

        if (
                couponUsageRepository
                        .existsByCoupon_IdAndOrder_Id(
                                coupon.getId(),
                                orderId
                        )
        ) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Coupon usage has already been recorded for this order"
            );
        }

        validateCouponAvailability(
                coupon,
                customerId,
                order.getSubtotal()
        );

        CouponUsage usage =
                CouponUsage.builder()
                        .coupon(coupon)
                        .customer(customer)
                        .order(order)
                        .discountAmount(
                                defaultMoney(
                                        discountAmount
                                )
                        )
                        .build();

        return couponUsageRepository.save(usage);
    }

    /*
     * =========================================================
     * CALCULATIONS
     * =========================================================
     */

    private BigDecimal calculateCartSubtotal(
            Long customerId
    ) {
        Cart cart =
                cartRepository
                        .findByCustomer_Id(customerId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Your cart is empty"
                                )
                        );

        if (
                cart.getItems() == null
                        || cart.getItems().isEmpty()
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Your cart is empty"
            );
        }

        BigDecimal subtotal = BigDecimal.ZERO;

        for (CartItem item : cart.getItems()) {
            if (
                    item == null
                            || item.getQuantity() <= 0
                            || item.getVariant() == null
            ) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "The cart contains an invalid item"
                );
            }

            ProductVariant variant =
                    item.getVariant();

            if (variant.getPrice() == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "A cart item does not have a valid price"
                );
            }

            subtotal =
                    subtotal.add(
                            variant.getPrice()
                                    .multiply(
                                            BigDecimal.valueOf(
                                                    item.getQuantity()
                                            )
                                    )
                    );
        }

        return subtotal.setScale(
                2,
                RoundingMode.HALF_UP
        );
    }

    private BigDecimal calculateDiscount(
            Coupon coupon,
            BigDecimal subtotal,
            BigDecimal deliveryFee
    ) {
        BigDecimal discount;

        if (
                coupon.getDiscountType()
                        == DiscountType.FREE_DELIVERY
        ) {
            discount = deliveryFee;

        } else if (
                coupon.getDiscountType()
                        == DiscountType.FIXED_AMOUNT
        ) {
            discount =
                    coupon.getDiscountValue()
                            .min(subtotal);

        } else {
            discount =
                    subtotal
                            .multiply(
                                    coupon.getDiscountValue()
                            )
                            .divide(
                                    BigDecimal.valueOf(100),
                                    2,
                                    RoundingMode.HALF_UP
                            );

            if (
                    coupon.getMaximumDiscountAmount()
                            != null
            ) {
                discount =
                        discount.min(
                                coupon.getMaximumDiscountAmount()
                        );
            }

            discount = discount.min(subtotal);
        }

        return discount
                .max(BigDecimal.ZERO)
                .setScale(
                        2,
                        RoundingMode.HALF_UP
                );
    }

    private void validateCouponAvailability(
            Coupon coupon,
            Long customerId,
            BigDecimal subtotal
    ) {
        LocalDateTime now = LocalDateTime.now();

        if (!coupon.isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "This coupon is inactive"
            );
        }

        if (
                coupon.getStartsAt() != null
                        && now.isBefore(
                        coupon.getStartsAt()
                )
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "This coupon is not active yet"
            );
        }

        if (
                coupon.getExpiresAt() != null
                        && now.isAfter(
                        coupon.getExpiresAt()
                )
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "This coupon has expired"
            );
        }

        BigDecimal minimum =
                defaultMoney(
                        coupon.getMinimumOrderAmount()
                );

        if (subtotal.compareTo(minimum) < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Your order must be at least ₦"
                            + minimum
                            + " to use this coupon"
            );
        }

        long totalUsage =
                couponUsageRepository
                        .countByCoupon_Id(
                                coupon.getId()
                        );

        if (
                coupon.getUsageLimit() != null
                        && totalUsage
                        >= coupon.getUsageLimit()
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "This coupon has reached its usage limit"
            );
        }

        long customerUsage =
                couponUsageRepository
                        .countByCoupon_IdAndCustomer_Id(
                                coupon.getId(),
                                customerId
                        );

        if (
                coupon.getPerCustomerLimit() != null
                        && customerUsage
                        >= coupon.getPerCustomerLimit()
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "You have reached the usage limit for this coupon"
            );
        }
    }

    /*
     * =========================================================
     * VALIDATION HELPERS
     * =========================================================
     */

    private void validateCouponValues(
            DiscountType discountType,
            BigDecimal discountValue,
            BigDecimal maximumDiscountAmount,
            BigDecimal minimumOrderAmount,
            Integer usageLimit,
            Integer perCustomerLimit,
            LocalDateTime startsAt,
            LocalDateTime expiresAt
    ) {
        if (discountType == null) {
            throw badRequest(
                    "Discount type is required"
            );
        }

        BigDecimal value =
                defaultMoney(discountValue);

        if (
                discountType
                        == DiscountType.PERCENTAGE
        ) {
            if (
                    value.compareTo(
                            BigDecimal.ZERO
                    ) <= 0
                            || value.compareTo(
                            BigDecimal.valueOf(100)
                    ) > 0
            ) {
                throw badRequest(
                        "Percentage discount must be greater than 0 and not exceed 100"
                );
            }

        } else if (
                discountType
                        == DiscountType.FIXED_AMOUNT
        ) {
            if (
                    value.compareTo(
                            BigDecimal.ZERO
                    ) <= 0
            ) {
                throw badRequest(
                        "Fixed discount must be greater than zero"
                );
            }
        }

        if (
                maximumDiscountAmount != null
                        && maximumDiscountAmount
                        .compareTo(
                                BigDecimal.ZERO
                        ) < 0
        ) {
            throw badRequest(
                    "Maximum discount cannot be negative"
            );
        }

        if (
                minimumOrderAmount != null
                        && minimumOrderAmount
                        .compareTo(
                                BigDecimal.ZERO
                        ) < 0
        ) {
            throw badRequest(
                    "Minimum order amount cannot be negative"
            );
        }

        if (
                usageLimit != null
                        && perCustomerLimit != null
                        && perCustomerLimit > usageLimit
        ) {
            throw badRequest(
                    "Per-customer limit cannot exceed the total usage limit"
            );
        }

        if (
                startsAt != null
                        && expiresAt != null
                        && !expiresAt.isAfter(startsAt)
        ) {
            throw badRequest(
                    "Coupon expiry must be after its start time"
            );
        }
    }

    private CouponResponse toResponse(
            Coupon coupon
    ) {
        return CouponResponse.from(
                coupon,
                couponUsageRepository
                        .countByCoupon_Id(
                                coupon.getId()
                        )
        );
    }

    private Integer calculateRemainingGlobalUses(
            Coupon coupon
    ) {
        if (coupon.getUsageLimit() == null) {
            return null;
        }

        long used =
                couponUsageRepository
                        .countByCoupon_Id(
                                coupon.getId()
                        );

        return Math.max(
                coupon.getUsageLimit()
                        - (int) used,
                0
        );
    }

    private Integer calculateRemainingCustomerUses(
            Coupon coupon,
            Long customerId
    ) {
        if (
                coupon.getPerCustomerLimit()
                        == null
        ) {
            return null;
        }

        long used =
                couponUsageRepository
                        .countByCoupon_IdAndCustomer_Id(
                                coupon.getId(),
                                customerId
                        );

        return Math.max(
                coupon.getPerCustomerLimit()
                        - (int) used,
                0
        );
    }

    private Coupon getCouponEntity(
            Long couponId
    ) {
        return couponRepository
                .findById(couponId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Coupon not found"
                        )
                );
    }

    private User getActiveAdmin(
            Long adminId
    ) {
        User admin =
                userRepository
                        .findById(adminId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Administrator account not found"
                                )
                        );

        if (admin.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only administrators can manage coupons"
            );
        }

        if (
                admin.getStatus()
                        != AccountStatus.ACTIVE
        ) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Administrator account is not active"
            );
        }

        return admin;
    }

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
                    "Only customers can use coupons"
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

    private BigDecimal normalizedDiscountValue(
            DiscountType discountType,
            BigDecimal value
    ) {
        if (
                discountType
                        == DiscountType.FREE_DELIVERY
        ) {
            return BigDecimal.ZERO
                    .setScale(2);
        }

        return defaultMoney(value);
    }

    private BigDecimal defaultMoney(
            BigDecimal value
    ) {
        return value == null
                ? BigDecimal.ZERO
                .setScale(2)
                : value.setScale(
                        2,
                        RoundingMode.HALF_UP
                );
    }

    private BigDecimal normalizeMoney(
            BigDecimal value
    ) {
        if (value == null) {
            return null;
        }

        return value.setScale(
                2,
                RoundingMode.HALF_UP
        );
    }

    private String normalizeCode(
            String code
    ) {
        if (
                code == null
                        || code.isBlank()
        ) {
            throw badRequest(
                    "Coupon code is required"
            );
        }

        return code.trim()
                .toUpperCase(Locale.ROOT);
    }

    private String clean(
            String value
    ) {
        if (
                value == null
                        || value.isBlank()
        ) {
            return null;
        }

        return value.trim();
    }

    private ResponseStatusException badRequest(
            String message
    ) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                message
        );
    }
}
