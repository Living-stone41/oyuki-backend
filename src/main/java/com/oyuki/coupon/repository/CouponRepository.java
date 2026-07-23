package com.oyuki.coupon.repository;

import com.oyuki.coupon.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CouponRepository
        extends JpaRepository<Coupon, Long> {

    Optional<Coupon> findByCodeIgnoreCase(
            String code
    );

    boolean existsByCodeIgnoreCase(
            String code
    );

    boolean existsByCodeIgnoreCaseAndIdNot(
            String code,
            Long couponId
    );

    List<Coupon> findAllByOrderByCreatedAtDesc();

    List<Coupon> findAllByActiveOrderByCreatedAtDesc(
            boolean active
    );
}
