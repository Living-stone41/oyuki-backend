package com.oyuki.tracking.repository;

import com.oyuki.tracking.entity.OrderDelivery;
import com.oyuki.tracking.enums.OrderDeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderDeliveryRepository
        extends JpaRepository<OrderDelivery, Long> {

    Optional<OrderDelivery> findByOrder_Id(
            Long orderId
    );

    Optional<OrderDelivery> findByTrackingNumberIgnoreCase(
            String trackingNumber
    );

    Optional<OrderDelivery> findByIdAndRider_Id(
            Long deliveryId,
            Long riderId
    );

    Optional<OrderDelivery> findByOrder_IdAndOrder_Customer_Id(
            Long orderId,
            Long customerId
    );

    List<OrderDelivery>
    findAllByOrder_Customer_IdOrderByCreatedAtDesc(
            Long customerId
    );

    List<OrderDelivery>
    findAllByRider_IdOrderByCreatedAtDesc(
            Long riderId
    );

    List<OrderDelivery>
    findAllByRider_IdAndStatusOrderByCreatedAtDesc(
            Long riderId,
            OrderDeliveryStatus status
    );

    List<OrderDelivery>
    findAllByStatusOrderByCreatedAtDesc(
            OrderDeliveryStatus status
    );

    List<OrderDelivery>
    findAllByOrderByCreatedAtDesc();

    boolean existsByOrder_Id(
            Long orderId
    );

    boolean existsByTrackingNumberIgnoreCase(
            String trackingNumber
    );
}