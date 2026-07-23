package com.oyuki.delivery.service;

import com.oyuki.address.entity.CustomerAddress;
import com.oyuki.address.repository.CustomerAddressRepository;
import com.oyuki.cart.entity.Cart;
import com.oyuki.cart.entity.CartItem;
import com.oyuki.cart.repository.CartRepository;
import com.oyuki.delivery.dto.DeliveryFeeResponse;
import com.oyuki.delivery.dto.ProviderDeliveryBreakdown;
import com.oyuki.delivery.entity.DeliveryRate;
import com.oyuki.delivery.enums.DeliveryRateType;
import com.oyuki.delivery.enums.NigeriaState;
import com.oyuki.delivery.repository.DeliveryRateRepository;
import com.oyuki.product.entity.Product;
import com.oyuki.providerlocation.entity.ProviderPickupAddress;
import com.oyuki.providerlocation.repository.ProviderPickupAddressRepository;
import com.oyuki.user.entity.User;
import com.oyuki.user.enums.AccountStatus;
import com.oyuki.user.enums.Role;
import com.oyuki.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DeliveryFeeService {

    private final CartRepository cartRepository;
    private final CustomerAddressRepository customerAddressRepository;
    private final ProviderPickupAddressRepository pickupAddressRepository;
    private final DeliveryRateRepository deliveryRateRepository;
    private final UserRepository userRepository;

    public DeliveryFeeService(
            CartRepository cartRepository,
            CustomerAddressRepository customerAddressRepository,
            ProviderPickupAddressRepository pickupAddressRepository,
            DeliveryRateRepository deliveryRateRepository,
            UserRepository userRepository
    ) {
        this.cartRepository = cartRepository;
        this.customerAddressRepository = customerAddressRepository;
        this.pickupAddressRepository = pickupAddressRepository;
        this.deliveryRateRepository = deliveryRateRepository;
        this.userRepository = userRepository;
    }

    /*
     * Calculates the fee for delivery to a customer's saved address.
     */
    @Transactional(readOnly = true)
    public DeliveryFeeResponse calculateCartDeliveryFee(
            Long customerId,
            Long addressId
    ) {
        getActiveCustomer(customerId);

        CustomerAddress address =
                customerAddressRepository
                        .findByIdAndCustomer_Id(
                                addressId,
                                customerId
                        )
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Delivery address not found"
                                )
                        );

        DestinationLocation destination =
                new DestinationLocation(
                        addressId,
                        parseState(address.getState()),
                        cleanRequired(
                                address.getCity(),
                                "Delivery city is missing"
                        ),
                        clean(address.getArea())
                );

        return calculateForDestination(
                customerId,
                destination
        );
    }

    /*
     * Calculates the fee when farm products are sent
     * to a destination kitchen.
     */
    @Transactional(readOnly = true)
    public DeliveryFeeResponse calculateCartDeliveryFeeToKitchen(
            Long customerId,
            Long kitchenId
    ) {
        getActiveCustomer(customerId);

        User kitchen =
                userRepository
                        .findById(kitchenId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Destination kitchen not found"
                                )
                        );

        if (kitchen.getRole() != Role.KITCHEN) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The selected destination is not a kitchen"
            );
        }

        if (kitchen.getStatus() != AccountStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The selected kitchen is not active"
            );
        }

        ProviderPickupAddress address =
                pickupAddressRepository
                        .findByProvider_IdAndActiveTrue(kitchenId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "The selected kitchen has not configured an active address"
                                )
                        );

        DestinationLocation destination =
                new DestinationLocation(
                        null,
                        address.getState(),
                        cleanRequired(
                                address.getCity(),
                                "Kitchen city is missing"
                        ),
                        clean(address.getArea())
                );

        return calculateForDestination(
                customerId,
                destination
        );
    }

    private DeliveryFeeResponse calculateForDestination(
            Long customerId,
            DestinationLocation destination
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

        Map<Long, User> providers =
                extractProviders(cart.getItems());

        if (providers.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The cart does not contain products from a valid provider"
            );
        }

        List<DeliveryRate> activeRates =
                deliveryRateRepository
                        .findAllByActiveTrueOrderByRateTypeAscOriginStateAscDestinationStateAsc();

        if (activeRates.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No active delivery rates have been configured"
            );
        }

        List<ProviderRoute> providerRoutes =
                providers.values()
                        .stream()
                        .map(provider ->
                                buildProviderRoute(
                                        provider,
                                        destination,
                                        activeRates
                                )
                        )
                        .toList();

        Map<LocationKey, List<ProviderRoute>> routesByLocation =
                providerRoutes.stream()
                        .collect(
                                Collectors.groupingBy(
                                        ProviderRoute::locationKey,
                                        LinkedHashMap::new,
                                        Collectors.toList()
                                )
                        );

        List<LocationCharge> locationCharges =
                routesByLocation.values()
                        .stream()
                        .map(routes -> {
                            ProviderRoute first = routes.get(0);

                            return new LocationCharge(
                                    first.locationKey(),
                                    first.rate(),
                                    first.fee(),
                                    routes
                            );
                        })
                        .toList();

        BigDecimal totalRouteFees =
                locationCharges.stream()
                        .map(LocationCharge::fee)
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        BigDecimal finalDeliveryFee =
                locationCharges.stream()
                        .map(LocationCharge::fee)
                        .max(BigDecimal::compareTo)
                        .orElse(BigDecimal.ZERO);

        BigDecimal providerPickupFees =
                totalRouteFees
                        .subtract(finalDeliveryFee)
                        .max(BigDecimal.ZERO);

        BigDecimal additionalProviderRate =
                findLatestRate(
                        activeRates.stream()
                                .filter(rate ->
                                        rate.getRateType()
                                                == DeliveryRateType.ADDITIONAL_PROVIDER
                                )
                                .toList()
                )
                        .map(DeliveryRate::getFee)
                        .orElse(BigDecimal.ZERO);

        int additionalProviders =
                Math.max(
                        providers.size() - 1,
                        0
                );

        BigDecimal additionalProviderFees =
                additionalProviderRate.multiply(
                        BigDecimal.valueOf(
                                additionalProviders
                        )
                );

        BigDecimal totalDeliveryFee =
                totalRouteFees.add(
                        additionalProviderFees
                );

        int estimatedMinDays =
                locationCharges.stream()
                        .map(LocationCharge::rate)
                        .map(DeliveryRate::getEstimatedMinDays)
                        .filter(Objects::nonNull)
                        .max(Integer::compareTo)
                        .orElse(0);

        int estimatedMaxDays =
                locationCharges.stream()
                        .map(LocationCharge::rate)
                        .map(DeliveryRate::getEstimatedMaxDays)
                        .filter(Objects::nonNull)
                        .max(Integer::compareTo)
                        .orElse(Math.max(1, estimatedMinDays));

        List<ProviderDeliveryBreakdown> breakdown =
                createBreakdown(
                        locationCharges,
                        destination
                );

        return new DeliveryFeeResponse(
                destination.addressId(),
                destination.state(),
                destination.city(),
                destination.area(),

                providers.size(),
                routesByLocation.size(),

                providerPickupFees,
                additionalProviderFees,
                finalDeliveryFee,
                totalDeliveryFee,

                estimatedMinDays,
                estimatedMaxDays,

                breakdown
        );
    }

    private Map<Long, User> extractProviders(
            List<CartItem> cartItems
    ) {
        Map<Long, User> providers =
                new LinkedHashMap<>();

        for (CartItem item : cartItems) {
            if (
                    item == null
                            || item.getVariant() == null
                            || item.getVariant().getProduct() == null
            ) {
                continue;
            }

            Product product =
                    item.getVariant().getProduct();

            User provider =
                    product.getOwner();

            if (
                    provider == null
                            || provider.getId() == null
            ) {
                continue;
            }

            providers.putIfAbsent(
                    provider.getId(),
                    provider
            );
        }

        return providers;
    }

    private ProviderRoute buildProviderRoute(
            User provider,
            DestinationLocation destination,
            List<DeliveryRate> activeRates
    ) {
        ProviderPickupAddress pickup =
                pickupAddressRepository
                        .findByProvider_IdAndActiveTrue(
                                provider.getId()
                        )
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        getDisplayName(provider)
                                                + " has not configured an active pickup address"
                                )
                        );

        LocationKey locationKey =
                new LocationKey(
                        pickup.getState(),
                        normalize(pickup.getCity()),
                        normalize(pickup.getArea())
                );

        DeliveryRate matchedRate =
                findBestRate(
                        pickup,
                        destination,
                        activeRates
                );

        return new ProviderRoute(
                provider,
                pickup,
                locationKey,
                matchedRate,
                matchedRate.getFee()
        );
    }

    private DeliveryRate findBestRate(
            ProviderPickupAddress origin,
            DestinationLocation destination,
            List<DeliveryRate> activeRates
    ) {
        boolean sameState =
                origin.getState()
                        == destination.state();

        boolean sameCity =
                sameState
                        && sameText(
                        origin.getCity(),
                        destination.city()
                );

        boolean sameArea =
                sameCity
                        && hasText(origin.getArea())
                        && hasText(destination.area())
                        && sameText(
                        origin.getArea(),
                        destination.area()
                );

        List<DeliveryRateType> priority;

        if (sameArea) {
            priority = List.of(
                    DeliveryRateType.SAME_AREA,
                    DeliveryRateType.SAME_CITY,
                    DeliveryRateType.SAME_STATE
            );

        } else if (sameCity) {
            priority = List.of(
                    DeliveryRateType.SAME_CITY,
                    DeliveryRateType.SAME_STATE
            );

        } else if (sameState) {
            priority = List.of(
                    DeliveryRateType.SAME_STATE
            );

        } else {
            priority = List.of(
                    DeliveryRateType.INTERSTATE
            );
        }

        for (DeliveryRateType rateType : priority) {
            List<DeliveryRate> matches =
                    activeRates.stream()
                            .filter(rate ->
                                    rate.getRateType() == rateType
                            )
                            .filter(rate ->
                                    rateMatches(
                                            rate,
                                            origin,
                                            destination
                                    )
                            )
                            .toList();

            Optional<DeliveryRate> bestMatch =
                    findBestMatchingRate(matches);

            if (bestMatch.isPresent()) {
                return bestMatch.get();
            }
        }

        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "No active delivery rate is configured from "
                        + origin.getState().getDisplayName()
                        + ", "
                        + origin.getCity()
                        + " to "
                        + destination.state().getDisplayName()
                        + ", "
                        + destination.city()
        );
    }

    private boolean rateMatches(
            DeliveryRate rate,
            ProviderPickupAddress origin,
            DestinationLocation destination
    ) {
        if (rate.getRateType() == DeliveryRateType.ADDITIONAL_PROVIDER) {
            return false;
        }

        if (
                rate.getOriginState() != null
                        && rate.getOriginState() != origin.getState()
        ) {
            return false;
        }

        if (
                rate.getDestinationState() != null
                        && rate.getDestinationState() != destination.state()
        ) {
            return false;
        }

        if (
                hasText(rate.getOriginCity())
                        && !sameText(
                        rate.getOriginCity(),
                        origin.getCity()
                )
        ) {
            return false;
        }

        if (
                hasText(rate.getDestinationCity())
                        && !sameText(
                        rate.getDestinationCity(),
                        destination.city()
                )
        ) {
            return false;
        }

        if (
                hasText(rate.getOriginArea())
                        && !sameText(
                        rate.getOriginArea(),
                        origin.getArea()
                )
        ) {
            return false;
        }

        if (
                hasText(rate.getDestinationArea())
                        && !sameText(
                        rate.getDestinationArea(),
                        destination.area()
                )
        ) {
            return false;
        }

        return true;
    }

    private Optional<DeliveryRate> findBestMatchingRate(
            List<DeliveryRate> rates
    ) {
        return rates.stream()
                .max(
                        Comparator
                                .comparingInt(
                                        this::rateSpecificity
                                )
                                .thenComparing(
                                        DeliveryRate::getId,
                                        Comparator.nullsFirst(
                                                Long::compareTo
                                        )
                                )
                );
    }

    private int rateSpecificity(
            DeliveryRate rate
    ) {
        int score = 0;

        if (rate.getOriginState() != null) {
            score += 8;
        }

        if (rate.getDestinationState() != null) {
            score += 8;
        }

        if (hasText(rate.getOriginCity())) {
            score += 4;
        }

        if (hasText(rate.getDestinationCity())) {
            score += 4;
        }

        if (hasText(rate.getOriginArea())) {
            score += 2;
        }

        if (hasText(rate.getDestinationArea())) {
            score += 2;
        }

        return score;
    }

    private Optional<DeliveryRate> findLatestRate(
            List<DeliveryRate> rates
    ) {
        return rates.stream()
                .max(
                        Comparator.comparing(
                                DeliveryRate::getId,
                                Comparator.nullsFirst(
                                        Long::compareTo
                                )
                        )
                );
    }

    private List<ProviderDeliveryBreakdown> createBreakdown(
            List<LocationCharge> locationCharges,
            DestinationLocation destination
    ) {
        List<ProviderDeliveryBreakdown> responses =
                new ArrayList<>();

        for (LocationCharge charge : locationCharges) {
            boolean firstProvider = true;

            for (ProviderRoute route : charge.routes()) {
                BigDecimal allocatedFee =
                        firstProvider
                                ? charge.fee()
                                : BigDecimal.ZERO;

                String message =
                        firstProvider
                                ? "Delivery rate applied once for this pickup location"
                                : "Provider shares a pickup location already included in the fee";

                responses.add(
                        new ProviderDeliveryBreakdown(
                                route.provider().getId(),
                                getDisplayName(
                                        route.provider()
                                ),

                                route.pickup().getState(),
                                route.pickup().getCity(),
                                route.pickup().getArea(),

                                destination.state(),
                                destination.city(),
                                destination.area(),

                                charge.rate().getRateType(),

                                allocatedFee,

                                message
                        )
                );

                firstProvider = false;
            }
        }

        return responses;
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

        if (customer.getRole() != Role.CUSTOMER) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only customers can calculate delivery fees"
            );
        }

        if (customer.getStatus() != AccountStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Customer account is not active"
            );
        }

        return customer;
    }

    private NigeriaState parseState(
            String state
    ) {
        try {
            NigeriaState parsed =
                    NigeriaState.fromValue(state);

            if (parsed == null) {
                throw new IllegalArgumentException();
            }

            return parsed;

        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Saved address contains an unsupported Nigerian state: "
                            + state
            );
        }
    }

    private String getDisplayName(
            User user
    ) {
        if (
                user == null
                        || user.getFullName() == null
                        || user.getFullName().isBlank()
        ) {
            return "Provider";
        }

        return user.getFullName().trim();
    }

    private String cleanRequired(
            String value,
            String message
    ) {
        String cleaned = clean(value);

        if (cleaned == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    message
            );
        }

        return cleaned;
    }

    private String clean(
            String value
    ) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private String normalize(
            String value
    ) {
        String cleaned = clean(value);

        return cleaned == null
                ? ""
                : cleaned.toLowerCase(Locale.ROOT);
    }

    private boolean hasText(
            String value
    ) {
        return clean(value) != null;
    }

    private boolean sameText(
            String first,
            String second
    ) {
        return normalize(first)
                .equals(normalize(second));
    }

    private record DestinationLocation(
            Long addressId,
            NigeriaState state,
            String city,
            String area
    ) {
    }

    private record LocationKey(
            NigeriaState state,
            String city,
            String area
    ) {
    }

    private record ProviderRoute(
            User provider,
            ProviderPickupAddress pickup,
            LocationKey locationKey,
            DeliveryRate rate,
            BigDecimal fee
    ) {
    }

    private record LocationCharge(
            LocationKey locationKey,
            DeliveryRate rate,
            BigDecimal fee,
            List<ProviderRoute> routes
    ) {
    }
}
