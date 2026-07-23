package com.oyuki.payment.repository;

import com.oyuki.payment.entity.PaymentProof;
import com.oyuki.payment.enums.PaymentProofStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentProofRepository
        extends JpaRepository<PaymentProof, Long> {

    List<PaymentProof>
    findAllByOrder_IdOrderByCreatedAtDesc(
            Long orderId
    );

    Optional<PaymentProof>
    findTopByOrder_IdOrderByCreatedAtDesc(
            Long orderId
    );

    List<PaymentProof>
    findAllByCustomer_IdOrderByCreatedAtDesc(
            Long customerId
    );

    Optional<PaymentProof>
    findByIdAndCustomer_Id(
            Long paymentProofId,
            Long customerId
    );

    List<PaymentProof>
    findAllByStatusOrderByCreatedAtDesc(
            PaymentProofStatus status
    );

    List<PaymentProof>
    findAllByOrderByCreatedAtDesc();

    boolean existsByTransactionReferenceIgnoreCase(
            String transactionReference
    );

    boolean existsByOrder_IdAndStatusIn(
            Long orderId,
            List<PaymentProofStatus> statuses
    );
}