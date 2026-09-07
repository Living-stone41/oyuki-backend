package com.oyuki.marketsquare.repository;

import com.oyuki.marketsquare.entity.MarketAgentProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MarketAgentProfileRepository extends JpaRepository<MarketAgentProfile, Long> {
    Optional<MarketAgentProfile> findByUserId(Long userId);
}
