package com.oyuki.refund.repository;

import com.oyuki.refund.entity.OrderRefund;
import com.oyuki.refund.enums.RefundStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRefundRepository
        extends JpaRepository<OrderRefund, Long> {

    List<OrderRefund>
    findAllByOrder_IdOrderByCreatedAtDesc(
            Long orderId
    );

    List<OrderRefund>
    findAllByCustomer_IdOrderByCreatedAtDesc(
            Long customerId
    );

    List<OrderRefund>
    findAllByStatusOrderByCreatedAtDesc(
            RefundStatus status
    );

    List<OrderRefund>
    findAllByOrderByCreatedAtDesc();

    Optional<OrderRefund>
    findByIdAndOrder_Id(
            Long refundId,
            Long orderId
    );

    boolean existsByOrder_IdAndStatusIn(
            Long orderId,
            List<RefundStatus> statuses
    );
}