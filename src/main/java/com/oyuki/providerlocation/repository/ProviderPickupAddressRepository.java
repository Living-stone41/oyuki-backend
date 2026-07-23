package com.oyuki.providerlocation.repository;

import com.oyuki.providerlocation.entity.ProviderPickupAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProviderPickupAddressRepository
        extends JpaRepository<ProviderPickupAddress, Long> {

    Optional<ProviderPickupAddress>
    findByProvider_Id(
            Long providerId
    );

    Optional<ProviderPickupAddress>
    findByProvider_IdAndActiveTrue(
            Long providerId
    );

    boolean existsByProvider_Id(
            Long providerId
    );

    List<ProviderPickupAddress>
    findAllByActiveTrueOrderByStateAscCityAscAreaAsc();
}
