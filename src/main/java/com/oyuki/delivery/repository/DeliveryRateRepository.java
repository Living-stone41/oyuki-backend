package com.oyuki.delivery.repository;

import com.oyuki.delivery.entity.DeliveryRate;
import com.oyuki.delivery.enums.DeliveryRateType;
import com.oyuki.delivery.enums.NigeriaState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeliveryRateRepository
        extends JpaRepository<DeliveryRate, Long> {

    List<DeliveryRate>
    findAllByOrderByRateTypeAscOriginStateAscDestinationStateAscCreatedAtDesc();

    List<DeliveryRate>
    findAllByActiveTrueOrderByRateTypeAscOriginStateAscDestinationStateAsc();

    List<DeliveryRate>
    findAllByRateTypeOrderByCreatedAtDesc(
            DeliveryRateType rateType
    );

    List<DeliveryRate>
    findAllByRateTypeAndActiveTrueOrderByCreatedAtDesc(
            DeliveryRateType rateType
    );

    List<DeliveryRate>
    findAllByRateTypeAndOriginStateAndDestinationStateAndActiveTrueOrderByCreatedAtDesc(
            DeliveryRateType rateType,
            NigeriaState originState,
            NigeriaState destinationState
    );
}
