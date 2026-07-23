package com.oyuki.providerlocation.service;

import com.oyuki.providerlocation.dto.ProviderPickupAddressResponse;
import com.oyuki.providerlocation.dto.SaveProviderPickupAddressRequest;
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

@Service
public class ProviderPickupAddressService {

    private final ProviderPickupAddressRepository pickupAddressRepository;
    private final UserRepository userRepository;

    public ProviderPickupAddressService(
            ProviderPickupAddressRepository pickupAddressRepository,
            UserRepository userRepository
    ) {
        this.pickupAddressRepository = pickupAddressRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ProviderPickupAddressResponse saveAddress(
            Long providerId,
            SaveProviderPickupAddressRequest request
    ) {
        User provider = getActiveProvider(providerId);

        ProviderPickupAddress address =
                pickupAddressRepository
                        .findByProvider_Id(providerId)
                        .orElseGet(ProviderPickupAddress::new);

        address.setProvider(provider);
        address.setCountry("Nigeria");
        address.setState(request.state());
        address.setCity(cleanRequired(request.city(), "City is required"));
        address.setLga(cleanRequired(request.lga(), "LGA is required"));
        address.setArea(cleanRequired(request.area(), "Area is required"));
        address.setStreetAddress(
                cleanRequired(
                        request.streetAddress(),
                        "Street address is required"
                )
        );
        address.setLandmark(clean(request.landmark()));
        address.setLatitude(request.latitude());
        address.setLongitude(request.longitude());
        address.setActive(
                request.active() == null
                        || request.active()
        );

        return ProviderPickupAddressResponse.from(
                pickupAddressRepository.save(address)
        );
    }

    @Transactional(readOnly = true)
    public ProviderPickupAddressResponse getMyAddress(
            Long providerId
    ) {
        getActiveProvider(providerId);

        ProviderPickupAddress address =
                pickupAddressRepository
                        .findByProvider_Id(providerId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Pickup address has not been created"
                                )
                        );

        return ProviderPickupAddressResponse.from(address);
    }

    private User getActiveProvider(
            Long providerId
    ) {
        User provider =
                userRepository
                        .findById(providerId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Provider account not found"
                                )
                        );

        if (
                provider.getRole() != Role.SELLER
                        && provider.getRole() != Role.KITCHEN
        ) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only sellers and kitchens can manage pickup addresses"
            );
        }

        if (
                provider.getStatus() != AccountStatus.ACTIVE
                        && provider.getStatus()
                        != AccountStatus.PENDING_APPROVAL
        ) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "This provider account cannot manage a pickup address"
            );
        }

        return provider;
    }

    private String cleanRequired(
            String value,
            String message
    ) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    message
            );
        }

        return value.trim();
    }

    private String clean(
            String value
    ) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}
