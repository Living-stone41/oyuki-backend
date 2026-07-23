package com.oyuki.farmersday.dto;

import com.oyuki.farmersday.entity.FarmersDayEvent;
import com.oyuki.farmersday.enums.FarmersDayStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record FarmersDayResponse(

        Long id,
        String title,
        String description,

        LocalDate eventDate,
        LocalTime startTime,
        LocalTime endTime,

        String offerDetails,
        String location,
        String bannerImageUrl,

        FarmersDayStatus status,

        Long createdById,
        String createdByName,

        LocalDateTime publishedAt,
        LocalDateTime startedAt,
        LocalDateTime completedAt,

        LocalDateTime createdAt,
        LocalDateTime updatedAt

) {

    public static FarmersDayResponse from(
            FarmersDayEvent event
    ) {
        return new FarmersDayResponse(
                event.getId(),
                event.getTitle(),
                event.getDescription(),

                event.getEventDate(),
                event.getStartTime(),
                event.getEndTime(),

                event.getOfferDetails(),
                event.getLocation(),
                event.getBannerImageUrl(),

                event.getStatus(),

                event.getCreatedBy().getId(),
                event.getCreatedBy().getFullName(),

                event.getPublishedAt(),
                event.getStartedAt(),
                event.getCompletedAt(),

                event.getCreatedAt(),
                event.getUpdatedAt()
        );
    }
}