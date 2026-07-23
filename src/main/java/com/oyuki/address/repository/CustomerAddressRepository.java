package com.oyuki.address.repository;

import com.oyuki.address.entity.CustomerAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerAddressRepository
        extends JpaRepository<CustomerAddress, Long> {

    List<CustomerAddress>
    findAllByCustomer_IdOrderByDefaultAddressDescCreatedAtDesc(
            Long customerId
    );

    Optional<CustomerAddress>
    findByIdAndCustomer_Id(
            Long addressId,
            Long customerId
    );

    Optional<CustomerAddress>
    findByCustomer_IdAndDefaultAddressTrue(
            Long customerId
    );

    long countByCustomer_Id(
            Long customerId
    );
}