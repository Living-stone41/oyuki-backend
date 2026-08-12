package com.oyuki.marketsquare.repository;

import com.oyuki.marketsquare.entity.LocalGovernment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LocalGovernmentRepository extends JpaRepository<LocalGovernment, Long> {
    List<LocalGovernment> findAllByActiveTrueOrderByNameAsc();
    List<LocalGovernment> findAllByStateIdAndActiveTrueOrderByNameAsc(Long stateId);
    Optional<LocalGovernment> findByStateIdAndNameIgnoreCase(Long stateId, String name);
}
