package com.oyuki.marketsquare.repository;

import com.oyuki.marketsquare.entity.State;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StateRepository extends JpaRepository<State, Long> {
    Optional<State> findByNameIgnoreCase(String name);
    List<State> findAllByActiveTrueOrderByNameAsc();
}
