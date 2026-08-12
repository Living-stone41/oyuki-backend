package com.oyuki.marketsquare.repository;

import com.oyuki.marketsquare.entity.Market;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MarketRepository extends JpaRepository<Market, Long> {
    List<Market> findAllByActiveTrueOrderByNameAsc();
    List<Market> findAllByLgaIdAndActiveTrueOrderByNameAsc(Long lgaId);
    List<Market> findAllByLgaStateIdAndActiveTrueOrderByNameAsc(Long stateId);
}
