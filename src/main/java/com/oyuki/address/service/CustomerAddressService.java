package com.oyuki.address.service;

import com.oyuki.address.dto.AddressResponse;
import com.oyuki.address.dto.CreateAddressRequest;
import com.oyuki.address.dto.UpdateAddressRequest;
import com.oyuki.address.entity.CustomerAddress;
import com.oyuki.address.repository.CustomerAddressRepository;
import com.oyuki.user.entity.User;
import com.oyuki.user.enums.AccountStatus;
import com.oyuki.user.enums.Role;
import com.oyuki.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class CustomerAddressService {

    private static final long MAX_ADDRESSES = 10;

    private final CustomerAddressRepository addressRepository;
    private final UserRepository userRepository;

    public CustomerAddressService(
            CustomerAddressRepository addressRepository,
            UserRepository userRepository
    ) {
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
    }

    /*
     * =========================================================
     * CREATE ADDRESS
     * =========================================================
     */

    @Transactional
    public AddressResponse createAddress(
            Long customerId,
            CreateAddressRequest request
    ) {
        User customer = getActiveCustomer(customerId);

        long addressCount =
                addressRepository.countByCustomer_Id(
                        customerId
                );

        if (addressCount >= MAX_ADDRESSES) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "You cannot save more than 10 addresses"
            );
        }

        boolean makeDefault =
                addressCount == 0 ||
                Boolean.TRUE.equals(
                        request.defaultAddress()
                );

        if (makeDefault) {
            clearCurrentDefault(customerId);
        }

        CustomerAddress address =
                CustomerAddress.builder()
                        .customer(customer)
                        .label(cleanRequired(request.label()))
                        .recipientName(
                                cleanRequired(
                                        request.recipientName()
                                )
                        )
                        .phone(cleanRequired(request.phone()))
                        .state(cleanRequired(request.state()))
                        .city(cleanRequired(request.city()))
                        .area(cleanRequired(request.area()))
                        .streetAddress(
                                cleanRequired(
                                        request.streetAddress()
                                )
                        )
                        .landmark(clean(request.landmark()))
                        .postalCode(clean(request.postalCode()))
                        .deliveryInstructions(
                                clean(
                                        request.deliveryInstructions()
                                )
                        )
                        .defaultAddress(makeDefault)
                        .build();

        return AddressResponse.from(
                addressRepository.save(address)
        );
    }

    /*
     * =========================================================
     * VIEW ADDRESSES
     * =========================================================
     */

    @Transactional(readOnly = true)
    public List<AddressResponse> getAddresses(
            Long customerId
    ) {
        getActiveCustomer(customerId);

        return addressRepository
                .findAllByCustomer_IdOrderByDefaultAddressDescCreatedAtDesc(
                        customerId
                )
                .stream()
                .map(AddressResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AddressResponse getAddress(
            Long customerId,
            Long addressId
    ) {
        getActiveCustomer(customerId);

        return AddressResponse.from(
                getCustomerAddress(
                        customerId,
                        addressId
                )
        );
    }

    /*
     * =========================================================
     * UPDATE ADDRESS
     * =========================================================
     */

    @Transactional
    public AddressResponse updateAddress(
            Long customerId,
            Long addressId,
            UpdateAddressRequest request
    ) {
        getActiveCustomer(customerId);

        CustomerAddress address =
                getCustomerAddress(
                        customerId,
                        addressId
                );

        address.setLabel(
                cleanRequired(request.label())
        );

        address.setRecipientName(
                cleanRequired(request.recipientName())
        );

        address.setPhone(
                cleanRequired(request.phone())
        );

        address.setState(
                cleanRequired(request.state())
        );

        address.setCity(
                cleanRequired(request.city())
        );

        address.setArea(
                cleanRequired(request.area())
        );

        address.setStreetAddress(
                cleanRequired(request.streetAddress())
        );

        address.setLandmark(
                clean(request.landmark())
        );

        address.setPostalCode(
                clean(request.postalCode())
        );

        address.setDeliveryInstructions(
                clean(request.deliveryInstructions())
        );

        if (
                Boolean.TRUE.equals(
                        request.defaultAddress()
                )
        ) {
            clearCurrentDefault(customerId);
            address.setDefaultAddress(true);

        } else if (
                Boolean.FALSE.equals(
                        request.defaultAddress()
                ) &&
                address.isDefaultAddress()
        ) {
            long addressCount =
                    addressRepository
                            .countByCustomer_Id(
                                    customerId
                            );

            /*
             * The customer's only address must
             * remain the default address.
             */
            if (addressCount > 1) {
                address.setDefaultAddress(false);

                CustomerAddress saved =
                        addressRepository.save(address);

                promoteAnotherAddress(
                        customerId,
                        saved.getId()
                );

                return AddressResponse.from(saved);
            }
        }

        return AddressResponse.from(
                addressRepository.save(address)
        );
    }

    /*
     * =========================================================
     * SET DEFAULT ADDRESS
     * =========================================================
     */

    @Transactional
    public AddressResponse setDefaultAddress(
            Long customerId,
            Long addressId
    ) {
        getActiveCustomer(customerId);

        CustomerAddress address =
                getCustomerAddress(
                        customerId,
                        addressId
                );

        if (address.isDefaultAddress()) {
            return AddressResponse.from(address);
        }

        clearCurrentDefault(customerId);

        address.setDefaultAddress(true);

        return AddressResponse.from(
                addressRepository.save(address)
        );
    }

    /*
     * =========================================================
     * DELETE ADDRESS
     * =========================================================
     */

    @Transactional
    public void deleteAddress(
            Long customerId,
            Long addressId
    ) {
        getActiveCustomer(customerId);

        CustomerAddress address =
                getCustomerAddress(
                        customerId,
                        addressId
                );

        boolean wasDefault =
                address.isDefaultAddress();

        List<CustomerAddress> existingAddresses =
                addressRepository
                        .findAllByCustomer_IdOrderByDefaultAddressDescCreatedAtDesc(
                                customerId
                        );

        addressRepository.delete(address);

        if (wasDefault) {
            existingAddresses.stream()
                    .filter(existing ->
                            !existing.getId()
                                    .equals(addressId)
                    )
                    .findFirst()
                    .ifPresent(nextAddress -> {
                        nextAddress.setDefaultAddress(true);
                        addressRepository.save(nextAddress);
                    });
        }
    }

    /*
     * =========================================================
     * HELPERS
     * =========================================================
     */

    private void clearCurrentDefault(
            Long customerId
    ) {
        addressRepository
                .findByCustomer_IdAndDefaultAddressTrue(
                        customerId
                )
                .ifPresent(currentDefault -> {
                    currentDefault.setDefaultAddress(false);
                    addressRepository.save(currentDefault);
                });
    }

    private void promoteAnotherAddress(
            Long customerId,
            Long excludedAddressId
    ) {
        addressRepository
                .findAllByCustomer_IdOrderByDefaultAddressDescCreatedAtDesc(
                        customerId
                )
                .stream()
                .filter(address ->
                        !address.getId()
                                .equals(excludedAddressId)
                )
                .findFirst()
                .ifPresent(address -> {
                    address.setDefaultAddress(true);
                    addressRepository.save(address);
                });
    }

    private CustomerAddress getCustomerAddress(
            Long customerId,
            Long addressId
    ) {
        return addressRepository
                .findByIdAndCustomer_Id(
                        addressId,
                        customerId
                )
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Address not found"
                        )
                );
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
                    "Only customers can manage delivery addresses"
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

    private String cleanRequired(
            String value
    ) {
        if (
                value == null ||
                value.isBlank()
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A required address field is missing"
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
}