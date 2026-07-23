package com.oyuki.coupon.repository;

import com.oyuki.coupon.entity.CouponUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CouponUsageRepository
        extends JpaRepository<CouponUsage, Long> {

    long countByCoupon_Id(
            Long couponId
    );

    long countByCoupon_IdAndCustomer_Id(
            Long couponId,
            Long customerId
    );

    boolean existsByCoupon_IdAndOrder_Id(
            Long couponId,
            Long orderId
    );

    List<CouponUsage>
    findAllByCoupon_IdOrderByUsedAtDesc(
            Long couponId
    );

    List<CouponUsage>
    findAllByCustomer_IdOrderByUsedAtDesc(
            Long customerId
    );
}
