package com.oyuki.order.service;

import com.oyuki.address.entity.CustomerAddress;
import com.oyuki.address.repository.CustomerAddressRepository;
import com.oyuki.cart.entity.Cart;
import com.oyuki.cart.entity.CartItem;
import com.oyuki.cart.repository.CartRepository;
import com.oyuki.delivery.dto.DeliveryFeeResponse;
import com.oyuki.delivery.service.DeliveryFeeService;
import com.oyuki.coupon.dto.CouponValidationResponse;
import com.oyuki.coupon.dto.ValidateCouponRequest;
import com.oyuki.coupon.service.CouponService;
import com.oyuki.notification.enums.NotificationType;
import com.oyuki.notification.service.NotificationService;
import com.oyuki.order.dto.CheckoutRequest;
import com.oyuki.order.dto.OrderItemResponse;
import com.oyuki.order.dto.OrderResponse;
import com.oyuki.order.dto.RejectOrderRequest;
import com.oyuki.order.entity.Order;
import com.oyuki.order.entity.OrderItem;
import com.oyuki.order.enums.DeliveryType;
import com.oyuki.order.enums.OrderItemStatus;
import com.oyuki.order.enums.OrderStatus;
import com.oyuki.order.enums.PaymentMethod;
import com.oyuki.order.enums.PaymentStatus;
import com.oyuki.order.repository.OrderItemRepository;
import com.oyuki.order.repository.OrderRepository;
import com.oyuki.product.entity.Product;
import com.oyuki.product.entity.ProductVariant;
import com.oyuki.product.enums.ProductStatus;
import com.oyuki.product.repository.ProductRepository;
import com.oyuki.product.repository.ProductVariantRepository;
import com.oyuki.user.entity.User;
import com.oyuki.user.enums.AccountStatus;
import com.oyuki.user.enums.Role;
import com.oyuki.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    private final CartRepository cartRepository;

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;

    private final UserRepository userRepository;

    private final CustomerAddressRepository customerAddressRepository;

    private final DeliveryFeeService deliveryFeeService;

    private final CouponService couponService;

    private final NotificationService notificationService;
    /*
     * =========================================================
     * CUSTOMER CHECKOUT
     * =========================================================
     */


    @Transactional
    public OrderResponse checkout(
            Long customerId,
            CheckoutRequest request
    ) {
        User customer =
                getActiveCustomer(customerId);

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
                cart.getItems() == null ||
                cart.getItems().isEmpty()
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Your cart is empty"
            );
        }

        validateCheckoutRequest(request);

        DeliveryType deliveryType =
                resolveDeliveryType(request);

        User destinationKitchen =
                getDestinationKitchen(
                        request,
                        deliveryType
                );

        DeliverySnapshot deliverySnapshot =
                resolveDeliverySnapshot(
                        customerId,
                        request,
                        deliveryType,
                        destinationKitchen
                );

        DeliveryFeeResponse deliveryFeeResponse;

        if (
                deliveryType
                        == DeliveryType.CUSTOMER_ADDRESS
        ) {
            deliveryFeeResponse =
                    deliveryFeeService
                            .calculateCartDeliveryFee(
                                    customerId,
                                    request.addressId()
                            );

        } else {
            deliveryFeeResponse =
                    deliveryFeeService
                            .calculateCartDeliveryFeeToKitchen(
                                    customerId,
                                    request.destinationKitchenId()
                            );
        }

        BigDecimal calculatedDeliveryFee =
                deliveryFeeResponse.totalDeliveryFee();

        String appliedCouponCode = null;

        BigDecimal calculatedDiscountAmount =
                BigDecimal.ZERO;

        if (
                request.couponCode() != null &&
                !request.couponCode().isBlank()
        ) {
            CouponValidationResponse couponValidation =
                    couponService.validateCoupon(
                            customerId,
                            new ValidateCouponRequest(
                                    request.couponCode(),
                                    deliveryType,
                                    request.addressId(),
                                    request.destinationKitchenId()
                            )
                    );

            appliedCouponCode =
                    couponValidation.code();

            calculatedDiscountAmount =
                    couponValidation.discountAmount();
        }

        PaymentStatus paymentStatus =
                determinePaymentStatus(
                        request.paymentMethod()
                );

        Order order =
                Order.builder()
                        .customer(customer)
                        .status(OrderStatus.PENDING)
                        .deliveryType(deliveryType)
                        .paymentMethod(request.paymentMethod())
                        .paymentStatus(paymentStatus)
                        .destinationKitchen(destinationKitchen)
                        .recipientName(
                                deliverySnapshot.recipientName()
                        )
                        .recipientPhone(
                                deliverySnapshot.recipientPhone()
                        )
                        .state(
                                deliverySnapshot.state()
                        )
                        .lga(
                                deliverySnapshot.lga()
                        )
                        .area(
                                deliverySnapshot.area()
                        )
                        .addressLine(
                                deliverySnapshot.addressLine()
                        )
                        .deliveryInstructions(
                                deliverySnapshot.deliveryInstructions()
                        )
                        .subtotal(BigDecimal.ZERO)
                        .deliveryFee(calculatedDeliveryFee)
                        .couponCode(appliedCouponCode)
                        .discountAmount(calculatedDiscountAmount)
                        .totalAmount(BigDecimal.ZERO)
                        .items(new ArrayList<>())
                        .build();

        Order savedOrder =
                orderRepository.save(order);

        BigDecimal subtotal =
                BigDecimal.ZERO;

        List<OrderItem> createdItems =
                new ArrayList<>();

        for (CartItem cartItem : cart.getItems()) {

            ProductVariant variant =
                    cartItem.getVariant();

            if (variant == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "A cart item does not have a valid product variant"
                );
            }

            ProductVariant managedVariant =
                    productVariantRepository
                            .findById(variant.getId())
                            .orElseThrow(() ->
                                    new ResponseStatusException(
                                            HttpStatus.NOT_FOUND,
                                            "Product variant not found"
                                    )
                            );

            Product product =
                    managedVariant.getProduct();

            validateProductForCheckout(
                    product,
                    managedVariant,
                    cartItem.getQuantity()
            );

            BigDecimal unitPrice =
                    managedVariant.getPrice();

            BigDecimal lineTotal =
                    unitPrice.multiply(
                            BigDecimal.valueOf(
                                    cartItem.getQuantity()
                            )
                    );

            OrderItem orderItem =
                    OrderItem.builder()
                            .order(savedOrder)
                            .owner(product.getOwner())
                            .product(product)
                            .variant(managedVariant)
                            .productName(product.getName())
                            .productType(
                                    product.getProductType()
                            )
                            .measurementValue(
                                    managedVariant
                                            .getMeasurementValue()
                            )
                            .measurementUnit(
                                    managedVariant
                                            .getMeasurementUnit()
                            )
                            .unitPrice(unitPrice)
                            .quantity(
                                    cartItem.getQuantity()
                            )
                            .lineTotal(lineTotal)
                            .status(
                                    OrderItemStatus.PENDING
                            )
                            .rejectionReason(null)
                            .build();

            createdItems.add(orderItem);

            subtotal =
                    subtotal.add(lineTotal);

            reduceVariantStock(
                    managedVariant,
                    cartItem.getQuantity()
            );
        }

        List<OrderItem> savedItems =
                orderItemRepository.saveAll(
                        createdItems
                );

        savedOrder.setItems(
                new ArrayList<>(savedItems)
        );

        savedOrder.setSubtotal(subtotal);

        BigDecimal deliveryFee =
                savedOrder.getDeliveryFee() == null
                        ? BigDecimal.ZERO
                        : savedOrder.getDeliveryFee();

        BigDecimal totalBeforeDiscount =
                subtotal.add(deliveryFee);

        BigDecimal discountAmount =
                savedOrder.getDiscountAmount() == null
                        ? BigDecimal.ZERO
                        : savedOrder.getDiscountAmount();

        discountAmount =
                discountAmount
                        .max(BigDecimal.ZERO)
                        .min(totalBeforeDiscount);

        savedOrder.setDiscountAmount(
                discountAmount
        );

        savedOrder.setTotalAmount(
                totalBeforeDiscount
                        .subtract(discountAmount)
        );

        savedOrder =
                orderRepository.save(savedOrder);

        if (
                savedOrder.getCouponCode() != null &&
                !savedOrder.getCouponCode().isBlank()
        ) {
            couponService.recordUsage(
                    customerId,
                    savedOrder.getId(),
                    savedOrder.getCouponCode(),
                    discountAmount
            );
        }

        cart.clearItems();
        cartRepository.save(cart);

        sendCheckoutNotifications(
                savedOrder,
                savedItems
        );

        return OrderResponse.from(savedOrder);
    }

    /*
     * =========================================================
     * CUSTOMER ORDER HISTORY
     * =========================================================
     */

    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(
            Long customerId
    ) {
        getActiveCustomer(customerId);

        return orderRepository
                .findAllByCustomer_IdOrderByCreatedAtDesc(
                        customerId
                )
                .stream()
                .map(OrderResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getMyOrder(
            Long customerId,
            Long orderId
    ) {
        getActiveCustomer(customerId);

        Order order = orderRepository
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

        return OrderResponse.from(order);
    }

    /*
     * =========================================================
     * SELLER AND KITCHEN ORDER ITEMS
     * =========================================================
     */

    @Transactional(readOnly = true)
    public List<OrderItemResponse> getProviderItems(
            Long providerId,
            OrderItemStatus status
    ) {
        getActiveProvider(providerId);

        List<OrderItem> items;

        if (status == null) {
            items = orderItemRepository
                    .findAllByOwner_IdOrderByCreatedAtDesc(
                            providerId
                    );
        } else {
            items = orderItemRepository
                    .findAllByOwner_IdAndStatusOrderByCreatedAtDesc(
                            providerId,
                            status
                    );
        }

        return items.stream()
                .map(OrderItemResponse::from)
                .toList();
    }

    /*
     * Provider accepts an order item.
     */
    @Transactional
    public OrderItemResponse acceptOrderItem(
            Long providerId,
            Long orderItemId
    ) {
        getActiveProvider(providerId);

        OrderItem orderItem =
                getProviderOrderItem(
                        providerId,
                        orderItemId
                );

        if (
                orderItem.getStatus()
                        != OrderItemStatus.PENDING
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only pending order items can be accepted"
            );
        }

        orderItem.setStatus(
                OrderItemStatus.ACCEPTED
        );

        orderItem.setRejectionReason(null);

        OrderItem savedItem =
                orderItemRepository.save(orderItem);

        recalculateOrderStatus(
                savedItem.getOrder()
        );

        notifyCustomerProviderAccepted(
                savedItem
        );

        return OrderItemResponse.from(savedItem);
    }

    /*
     * Provider rejects an order item.
     */
    @Transactional
    public OrderItemResponse rejectOrderItem(
            Long providerId,
            Long orderItemId,
            RejectOrderRequest request
    ) {
        getActiveProvider(providerId);

        OrderItem orderItem =
                getProviderOrderItem(
                        providerId,
                        orderItemId
                );

        if (
                orderItem.getStatus()
                        != OrderItemStatus.PENDING
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only pending order items can be rejected"
            );
        }

        String rejectionReason =
                request == null ||
                request.reason() == null ||
                request.reason().isBlank()
                        ? "Provider could not fulfil the order"
                        : request.reason().trim();

        orderItem.setStatus(
                OrderItemStatus.REJECTED
        );

        orderItem.setRejectionReason(
                rejectionReason
        );

        restoreVariantStock(orderItem);

        OrderItem savedItem =
                orderItemRepository.save(orderItem);

        recalculateOrderTotals(
                savedItem.getOrder()
        );

        recalculateOrderStatus(
                savedItem.getOrder()
        );

        notifyCustomerProviderRejected(
                savedItem,
                rejectionReason
        );

        return OrderItemResponse.from(savedItem);
    }

    /*
     * Provider starts preparing the item.
     */
    @Transactional
    public OrderItemResponse markOrderItemProcessing(
            Long providerId,
            Long orderItemId
    ) {
        getActiveProvider(providerId);

        OrderItem orderItem =
                getProviderOrderItem(
                        providerId,
                        orderItemId
                );

        if (
                orderItem.getStatus()
                        != OrderItemStatus.ACCEPTED
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only accepted order items can be processed"
            );
        }

        orderItem.setStatus(
                OrderItemStatus.PROCESSING
        );

        OrderItem savedItem =
                orderItemRepository.save(orderItem);

        recalculateOrderStatus(
                savedItem.getOrder()
        );

        return OrderItemResponse.from(savedItem);
    }

    /*
     * Provider marks item ready for pickup.
     */
    @Transactional
    public OrderItemResponse markOrderItemReady(
            Long providerId,
            Long orderItemId
    ) {
        getActiveProvider(providerId);

        OrderItem orderItem =
                getProviderOrderItem(
                        providerId,
                        orderItemId
                );

        if (
                orderItem.getStatus()
                        != OrderItemStatus.PROCESSING
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only processing order items can be marked ready"
            );
        }

        orderItem.setStatus(
                OrderItemStatus.READY_FOR_PICKUP
        );

        OrderItem savedItem =
                orderItemRepository.save(orderItem);

        recalculateOrderStatus(
                savedItem.getOrder()
        );

        return OrderItemResponse.from(savedItem);
    }

    /*
     * =========================================================
     * COMPATIBILITY METHODS
     *
     * These methods are included in case your
     * OrderController uses the older method names.
     * =========================================================
     */

    @Transactional
    public OrderItemResponse acceptProviderItem(
            Long providerId,
            Long orderItemId
    ) {
        return acceptOrderItem(
                providerId,
                orderItemId
        );
    }

    @Transactional
    public OrderItemResponse rejectProviderItem(
            Long providerId,
            Long orderItemId,
            RejectOrderRequest request
    ) {
        return rejectOrderItem(
                providerId,
                orderItemId,
                request
        );
    }

    @Transactional
    public OrderItemResponse markProviderItemProcessing(
            Long providerId,
            Long orderItemId
    ) {
        return markOrderItemProcessing(
                providerId,
                orderItemId
        );
    }

    @Transactional
    public OrderItemResponse markProviderItemReady(
            Long providerId,
            Long orderItemId
    ) {
        return markOrderItemReady(
                providerId,
                orderItemId
        );
    }

    /*
     * =========================================================
     * CHECKOUT NOTIFICATIONS
     * =========================================================
     */

    private void sendCheckoutNotifications(
            Order order,
            List<OrderItem> orderItems
    ) {
        /*
         * Notify the customer.
         */
        notificationService.sendNotification(
                order.getCustomer(),
                NotificationType.ORDER_PLACED,
                "Order placed successfully",
                "Your order " +
                        order.getOrderNumber() +
                        " has been placed successfully.",
                "ORDER",
                order.getId(),
                "/customer/orders/" +
                        order.getId(),
                null
        );

        /*
         * Notify each seller or kitchen.
         */
        for (OrderItem item : orderItems) {

            notificationService.sendNotification(
                    item.getOwner(),
                    NotificationType.NEW_ORDER_RECEIVED,
                    "New order received",
                    "A customer ordered " +
                            item.getQuantity() +
                            " × " +
                            item.getProductName() +
                            ". Please review and accept or reject it.",
                    "ORDER_ITEM",
                    item.getId(),
                    "/provider/orders",
                    null
            );
        }
    }

    /*
     * =========================================================
     * PROVIDER ACCEPT/REJECT NOTIFICATIONS
     * =========================================================
     */

    private void notifyCustomerProviderAccepted(
            OrderItem orderItem
    ) {
        Order order =
                orderItem.getOrder();

        String providerName =
                getDisplayName(
                        orderItem.getOwner(),
                        "The provider"
                );

        notificationService.sendNotification(
                order.getCustomer(),
                NotificationType.PROVIDER_ACCEPTED,
                "Order item accepted",
                providerName +
                        " accepted your order for " +
                        orderItem.getQuantity() +
                        " × " +
                        orderItem.getProductName() +
                        ".",
                "ORDER_ITEM",
                orderItem.getId(),
                "/customer/orders/" +
                        order.getId(),
                null
        );
    }

    private void notifyCustomerProviderRejected(
            OrderItem orderItem,
            String rejectionReason
    ) {
        Order order =
                orderItem.getOrder();

        String providerName =
                getDisplayName(
                        orderItem.getOwner(),
                        "The provider"
                );

        String reason =
                rejectionReason == null ||
                rejectionReason.isBlank()
                        ? "No reason was provided."
                        : rejectionReason.trim();

        notificationService.sendNotification(
                order.getCustomer(),
                NotificationType.PROVIDER_REJECTED,
                "Order item rejected",
                providerName +
                        " rejected your order for " +
                        orderItem.getQuantity() +
                        " × " +
                        orderItem.getProductName() +
                        ". Reason: " +
                        reason,
                "ORDER_ITEM",
                orderItem.getId(),
                "/customer/orders/" +
                        order.getId(),
                null
        );
    }

    /*
     * =========================================================
     * ORDER STATUS AND TOTAL CALCULATIONS
     * =========================================================
     */

    private void recalculateOrderTotals(
            Order order
    ) {
        List<OrderItem> orderItems =
                orderItemRepository
                        .findAllByOrder_IdOrderByCreatedAtAsc(
                                order.getId()
                        );

        BigDecimal subtotal =
                orderItems.stream()
                        .filter(item ->
                                item.getStatus()
                                        != OrderItemStatus.REJECTED
                                &&
                                item.getStatus()
                                        != OrderItemStatus.CANCELLED
                        )
                        .map(OrderItem::getLineTotal)
                        .filter(value -> value != null)
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        BigDecimal deliveryFee =
                order.getDeliveryFee() == null
                        ? BigDecimal.ZERO
                        : order.getDeliveryFee();

        order.setSubtotal(subtotal);

        BigDecimal totalBeforeDiscount =
                subtotal.add(deliveryFee);

        BigDecimal discountAmount =
                order.getDiscountAmount() == null
                        ? BigDecimal.ZERO
                        : order.getDiscountAmount();

        discountAmount =
                discountAmount
                        .max(BigDecimal.ZERO)
                        .min(totalBeforeDiscount);

        order.setDiscountAmount(
                discountAmount
        );

        order.setTotalAmount(
                totalBeforeDiscount
                        .subtract(discountAmount)
        );

        orderRepository.save(order);
    }

    private void recalculateOrderStatus(
            Order order
    ) {
        List<OrderItem> orderItems =
                orderItemRepository
                        .findAllByOrder_IdOrderByCreatedAtAsc(
                                order.getId()
                        );

        List<OrderItem> activeItems =
                orderItems.stream()
                        .filter(item ->
                                item.getStatus()
                                        != OrderItemStatus.REJECTED
                                &&
                                item.getStatus()
                                        != OrderItemStatus.CANCELLED
                        )
                        .toList();

        if (activeItems.isEmpty()) {
            order.setStatus(
                    OrderStatus.REJECTED
            );

            orderRepository.save(order);
            return;
        }

        boolean allDelivered =
                activeItems.stream()
                        .allMatch(item ->
                                item.getStatus()
                                        == OrderItemStatus.DELIVERED
                        );

        if (allDelivered) {
            order.setStatus(
                    OrderStatus.DELIVERED
            );

            orderRepository.save(order);
            return;
        }

        /*
         * Do not move a delivery backwards once the
         * rider has started or completed it.
         */
        if (
                order.getStatus()
                        == OrderStatus.OUT_FOR_DELIVERY ||
                order.getStatus()
                        == OrderStatus.DELIVERED
        ) {
            return;
        }

        boolean allReady =
                activeItems.stream()
                        .allMatch(item ->
                                item.getStatus()
                                        == OrderItemStatus.READY_FOR_PICKUP
                                ||
                                item.getStatus()
                                        == OrderItemStatus.DELIVERED
                        );

        if (allReady) {
            order.setStatus(
                    OrderStatus.READY_FOR_PICKUP
            );

            orderRepository.save(order);
            return;
        }

        boolean anyProcessing =
                activeItems.stream()
                        .anyMatch(item ->
                                item.getStatus()
                                        == OrderItemStatus.PROCESSING
                        );

        if (anyProcessing) {
            order.setStatus(
                    OrderStatus.PROCESSING
            );

            orderRepository.save(order);
            return;
        }

        boolean anyAccepted =
                activeItems.stream()
                        .anyMatch(item ->
                                item.getStatus()
                                        == OrderItemStatus.ACCEPTED
                                ||
                                item.getStatus()
                                        == OrderItemStatus.READY_FOR_PICKUP
                        );

        if (anyAccepted) {
            order.setStatus(
                    OrderStatus.CONFIRMED
            );
        } else {
            order.setStatus(
                    OrderStatus.PENDING
            );
        }

        orderRepository.save(order);
    }

    /*
     * =========================================================
     * STOCK MANAGEMENT
     * =========================================================
     */

    private void validateProductForCheckout(
            Product product,
            ProductVariant variant,
            int requestedQuantity
    ) {
        if (product == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Product not found"
            );
        }

        if (
                product.getStatus()
                        != ProductStatus.ACTIVE
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    product.getName() +
                            " is not currently available"
            );
        }

        if (!variant.isAvailable()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    product.getName() +
                            " is not currently available"
            );
        }

        if (requestedQuantity <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Order quantity must be greater than zero"
            );
        }

        if (
                variant.getStockQuantity()
                        < requestedQuantity
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Insufficient stock for " +
                            product.getName()
            );
        }

        User owner = product.getOwner();

        if (owner == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    product.getName() +
                            " does not have a valid provider"
            );
        }

        if (
                owner.getRole() != Role.SELLER &&
                owner.getRole() != Role.KITCHEN
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    product.getName() +
                            " does not belong to a valid provider"
            );
        }

        if (
                owner.getStatus()
                        != AccountStatus.ACTIVE
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The provider for " +
                            product.getName() +
                            " is not active"
            );
        }
    }

    private void reduceVariantStock(
            ProductVariant variant,
            int quantity
    ) {
        int updatedStock =
                variant.getStockQuantity()
                        - quantity;

        variant.setStockQuantity(updatedStock);

        if (updatedStock <= 0) {
            variant.setStockQuantity(0);
            variant.setAvailable(false);
        }

        productVariantRepository.save(variant);

        updateProductAvailability(
                variant.getProduct()
        );
    }

    private void restoreVariantStock(
            OrderItem orderItem
    ) {
        ProductVariant variant =
                orderItem.getVariant();

        if (variant == null) {
            return;
        }

        int currentStock =
                variant.getStockQuantity();

        variant.setStockQuantity(
                currentStock +
                        orderItem.getQuantity()
        );

        variant.setAvailable(true);

        productVariantRepository.save(variant);

        Product product =
                variant.getProduct();

        if (
                product != null &&
                product.getStatus()
                        == ProductStatus.OUT_OF_STOCK
        ) {
            product.setStatus(
                    ProductStatus.ACTIVE
            );

            productRepository.save(product);
        }
    }

    private void updateProductAvailability(
            Product product
    ) {
        if (product == null) {
            return;
        }

        boolean hasAvailableVariant =
                product.getVariants() != null &&
                product.getVariants()
                        .stream()
                        .anyMatch(variant ->
                                variant.isAvailable()
                                &&
                                variant.getStockQuantity() > 0
                        );

        if (!hasAvailableVariant) {
            product.setStatus(
                    ProductStatus.OUT_OF_STOCK
            );

            productRepository.save(product);
        }
    }

    /*
     * =========================================================
     * USER AND ORDER ITEM VALIDATION
     * =========================================================
     */

    private User getActiveCustomer(
            Long customerId
    ) {
        User customer =
                userRepository.findById(customerId)
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
                    "Only customers can perform this action"
            );
        }

        if (
                customer.getStatus()
                        != AccountStatus.ACTIVE
        ) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Your customer account is not active"
            );
        }

        return customer;
    }

    private User getActiveProvider(
            Long providerId
    ) {
        User provider =
                userRepository.findById(providerId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Provider account not found"
                                )
                        );

        if (
                provider.getRole() != Role.SELLER &&
                provider.getRole() != Role.KITCHEN
        ) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only sellers and kitchens can perform this action"
            );
        }

        if (
                provider.getStatus()
                        != AccountStatus.ACTIVE
        ) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Your provider account is not active"
            );
        }

        return provider;
    }

    private OrderItem getProviderOrderItem(
            Long providerId,
            Long orderItemId
    ) {
        return orderItemRepository
                .findByIdAndOwner_Id(
                        orderItemId,
                        providerId
                )
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Order item not found"
                        )
                );
    }


    private User getDestinationKitchen(
            CheckoutRequest request,
            DeliveryType deliveryType
    ) {
        if (
                deliveryType
                        != DeliveryType.KITCHEN_ADDRESS
        ) {
            return null;
        }

        if (request.destinationKitchenId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Destination kitchen is required"
            );
        }

        User kitchen =
                userRepository
                        .findById(
                                request.destinationKitchenId()
                        )
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Destination kitchen not found"
                                )
                        );

        if (
                kitchen.getRole()
                        != Role.KITCHEN
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The selected destination is not a kitchen"
            );
        }

        if (
                kitchen.getStatus()
                        != AccountStatus.ACTIVE
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The selected kitchen is not active"
            );
        }

        return kitchen;
    }

    /*
     * =========================================================
     * CHECKOUT REQUEST VALIDATION
     * =========================================================
     */


    private void validateCheckoutRequest(
            CheckoutRequest request
    ) {
        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Checkout request is required"
            );
        }

        if (request.paymentMethod() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Payment method is required"
            );
        }

        DeliveryType deliveryType =
                resolveDeliveryType(request);

        if (
                deliveryType
                        == DeliveryType.CUSTOMER_ADDRESS &&
                request.addressId() == null
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Select a saved delivery address"
            );
        }

        if (
                deliveryType
                        == DeliveryType.KITCHEN_ADDRESS &&
                request.destinationKitchenId() == null
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Select a destination kitchen"
            );
        }
    }

    private PaymentStatus determinePaymentStatus(
            PaymentMethod paymentMethod
    ) {
        if (
                paymentMethod
                        == PaymentMethod.BANK_TRANSFER
        ) {
            return PaymentStatus
                    .AWAITING_CONFIRMATION;
        }

        return PaymentStatus.PENDING;
    }



    /*
     * =========================================================
     * SAVED ADDRESS CHECKOUT HELPERS
     * =========================================================
     */

    private DeliveryType resolveDeliveryType(
            CheckoutRequest request
    ) {
        if (request.deliveryType() == null) {
            return DeliveryType.CUSTOMER_ADDRESS;
        }

        return request.deliveryType();
    }

    private DeliverySnapshot resolveDeliverySnapshot(
            Long customerId,
            CheckoutRequest request,
            DeliveryType deliveryType,
            User destinationKitchen
    ) {
        if (
                deliveryType
                        == DeliveryType.CUSTOMER_ADDRESS
        ) {
            return resolveCustomerAddressSnapshot(
                    customerId,
                    request
            );
        }

        return resolveKitchenAddressSnapshot(
                request,
                destinationKitchen
        );
    }

    private DeliverySnapshot resolveCustomerAddressSnapshot(
            Long customerId,
            CheckoutRequest request
    ) {
        CustomerAddress address =
                customerAddressRepository
                        .findByIdAndCustomer_Id(
                                request.addressId(),
                                customerId
                        )
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Delivery address not found"
                                )
                        );

        String instructions =
                clean(
                        request.deliveryInstructions()
                );

        if (instructions == null) {
            instructions =
                    clean(
                            address.getDeliveryInstructions()
                    );
        }

        return new DeliverySnapshot(
                cleanRequired(
                        address.getRecipientName(),
                        "Address recipient name is missing"
                ),
                cleanRequired(
                        address.getPhone(),
                        "Address phone number is missing"
                ),
                cleanRequired(
                        address.getState(),
                        "Address state is missing"
                ),
                cleanRequired(
                        address.getCity(),
                        "Address city is missing"
                ),
                clean(address.getArea()),
                cleanRequired(
                        address.getStreetAddress(),
                        "Street address is missing"
                ),
                instructions
        );
    }

    private DeliverySnapshot resolveKitchenAddressSnapshot(
            CheckoutRequest request,
            User destinationKitchen
    ) {
        if (destinationKitchen == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Destination kitchen is required"
            );
        }

        String recipientName =
                clean(
                        request.recipientName()
                );

        if (recipientName == null) {
            recipientName =
                    clean(
                            destinationKitchen.getFullName()
                    );
        }

        return new DeliverySnapshot(
                cleanRequired(
                        recipientName,
                        "Kitchen recipient name is required"
                ),
                cleanRequired(
                        request.recipientPhone(),
                        "Kitchen recipient phone number is required"
                ),
                cleanRequired(
                        request.state(),
                        "Kitchen delivery state is required"
                ),
                cleanRequired(
                        request.lga(),
                        "Kitchen delivery LGA is required"
                ),
                clean(request.area()),
                cleanRequired(
                        request.addressLine(),
                        "Kitchen delivery address is required"
                ),
                clean(
                        request.deliveryInstructions()
                )
        );
    }

    private record DeliverySnapshot(
            String recipientName,
            String recipientPhone,
            String state,
            String lga,
            String area,
            String addressLine,
            String deliveryInstructions
    ) {
    }

    /*
     * =========================================================
     * SMALL HELPERS
     * =========================================================
     */

    private String cleanRequired(
            String value,
            String errorMessage
    ) {
        if (
                value == null ||
                value.isBlank()
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    errorMessage
            );
        }

        return value.trim();
    }

    private String clean(
            String value
    ) {
        if (
                value == null ||
                value.isBlank()
        ) {
            return null;
        }

        return value.trim();
    }

    private String getDisplayName(
            User user,
            String fallback
    ) {
        if (user == null) {
            return fallback;
        }

        if (
                user.getFullName() == null ||
                user.getFullName().isBlank()
        ) {
            return fallback;
        }

        return user.getFullName().trim();
    }
}
