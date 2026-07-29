package com.oyuki.referral.repository;

import com.oyuki.referral.entity.Referral;
import com.oyuki.referral.enums.ReferralStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReferralRepository extends JpaRepository<Referral, Long> {
    boolean existsByReferredUser_Id(Long referredUserId);
    Optional<Referral> findByReferredUser_Id(Long referredUserId);
    List<Referral> findAllByReferrer_IdOrderByCreatedAtDesc(Long referrerId);
    long countByReferrer_Id(Long referrerId);
    long countByReferrer_IdAndStatusIn(Long referrerId, Collection<ReferralStatus> statuses);
}
