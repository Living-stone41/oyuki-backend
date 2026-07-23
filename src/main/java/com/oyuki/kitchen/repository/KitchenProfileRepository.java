package com.oyuki.kitchen.repository;

import com.oyuki.kitchen.entity.KitchenProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface KitchenProfileRepository
        extends JpaRepository<KitchenProfile, Long> {

    Optional<KitchenProfile> findByUserId(Long userId);

    boolean existsByUserId(Long userId);
}