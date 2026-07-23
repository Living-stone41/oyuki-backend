package com.oyuki.logistics.repository;

import com.oyuki.logistics.entity.Delivery;
import com.oyuki.logistics.enums.DeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryRepository
        extends JpaRepository<Delivery, Long> {

    /*
     * Prevent creating two deliveries
     * for the same order item.
     */
    boolean existsByOrderItem_Id(Long orderItemId);

    /*
     * Find delivery using its order item.
     */
    Optional<Delivery> findByOrderItem_Id(
            Long orderItemId
    );

    /*
     * Logistics admin views deliveries
     * by their current status.
     */
    List<Delivery> findAllByStatusOrderByCreatedAtDesc(
            DeliveryStatus status
    );

    /*
     * Logistics admin views all deliveries.
     */
    List<Delivery> findAllByOrderByCreatedAtDesc();

    /*
     * Rider views all assigned deliveries.
     */
    List<Delivery> findAllByRider_IdOrderByCreatedAtDesc(
            Long riderId
    );

    /*
     * Rider filters assigned deliveries by status.
     */
    List<Delivery> findAllByRider_IdAndStatusOrderByCreatedAtDesc(
            Long riderId,
            DeliveryStatus status
    );

    /*
     * Ensures the logged-in rider owns the delivery.
     */
    Optional<Delivery> findByIdAndRider_Id(
            Long deliveryId,
            Long riderId
    );

    /*
     * Customer views deliveries belonging
     * to their own orders.
     */
    List<Delivery>
    findAllByOrder_Customer_IdOrderByCreatedAtDesc(
            Long customerId
    );

    /*
     * Public tracking lookup.
     */
    Optional<Delivery> findByTrackingNumber(
            String trackingNumber
    );
}