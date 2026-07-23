package com.oyuki.order.repository;

import com.oyuki.order.entity.Order;
import com.oyuki.order.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository
        extends JpaRepository<Order, Long> {

    /*
     * Customer order history.
     */
    List<Order> findAllByCustomer_IdOrderByCreatedAtDesc(
            Long customerId
    );

    /*
     * Find one order belonging to a customer.
     */
    Optional<Order> findByIdAndCustomer_Id(
            Long orderId,
            Long customerId
    );

    /*
     * Find an order using its public order number.
     */
    Optional<Order> findByOrderNumberAndCustomer_Id(
            String orderNumber,
            Long customerId
    );

    /*
     * Admin can view every order.
     */
    List<Order> findAllByOrderByCreatedAtDesc();

    /*
     * Admin can filter orders by status.
     */
    List<Order> findAllByStatusOrderByCreatedAtDesc(
            OrderStatus status
    );
    
    long countByStatus(OrderStatus status);

}