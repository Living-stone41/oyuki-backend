package com.oyuki.farmersday.repository;

import com.oyuki.farmersday.entity.FarmersDayEvent;
import com.oyuki.farmersday.enums.FarmersDayStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FarmersDayRepository
        extends JpaRepository<FarmersDayEvent, Long> {

    List<FarmersDayEvent>
    findAllByOrderByEventDateDescStartTimeDesc();

    List<FarmersDayEvent>
    findAllByStatusOrderByEventDateAscStartTimeAsc(
            FarmersDayStatus status
    );

    List<FarmersDayEvent>
    findAllByEventDateGreaterThanEqualAndStatusInOrderByEventDateAscStartTimeAsc(
            LocalDate date,
            List<FarmersDayStatus> statuses
    );

    Optional<FarmersDayEvent>
    findFirstByStatusOrderByEventDateAscStartTimeAsc(
            FarmersDayStatus status
    );
    List<FarmersDayEvent> findAllByEventDateAndStatus(
        LocalDate eventDate,
        FarmersDayStatus status
);
}   