package com.oyuki.order.repository;

import com.oyuki.order.entity.OrderItem;
import com.oyuki.order.enums.OrderItemStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderItemRepository
        extends JpaRepository<OrderItem, Long> {

    /*
     * Get all items belonging to one order.
     */
    List<OrderItem> findAllByOrder_IdOrderByCreatedAtAsc(
            Long orderId
    );

    /*
     * Seller or kitchen order dashboard.
     */
    List<OrderItem> findAllByOwner_IdOrderByCreatedAtDesc(
        
            Long ownerId
    );

    /*
     * Seller or kitchen filters items by status.
     */
    List<OrderItem> findAllByOwner_IdAndStatusOrderByCreatedAtDesc(
            Long ownerId,
            OrderItemStatus status
    );

    /*
     * Make sure the logged-in seller or kitchen owns the item.
     */
    Optional<OrderItem> findByIdAndOwner_Id(
            Long orderItemId,
            Long ownerId
    );

    /*
     * Used when calculating the overall order status.
     */
    boolean existsByOrder_IdAndStatus(
            Long orderId,
            OrderItemStatus status
    );
    List<OrderItem> findAllByStatusOrderByCreatedAtDesc(
        OrderItemStatus status
);
}