package com.oyuki.farmersday.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public record CreateFarmersDayRequest(

        @NotBlank(message = "Event title is required")
        @Size(
                max = 200,
                message = "Event title cannot exceed 200 characters"
        )
        String title,

        @NotBlank(message = "Event description is required")
        @Size(
                max = 4000,
                message = "Description cannot exceed 4000 characters"
        )
        String description,

        @NotNull(message = "Event date is required")
        @FutureOrPresent(
                message = "Event date cannot be in the past"
        )
        LocalDate eventDate,

        @NotNull(message = "Start time is required")
        LocalTime startTime,

        @NotNull(message = "End time is required")
        LocalTime endTime,

        @Size(
                max = 2000,
                message = "Offer details cannot exceed 2000 characters"
        )
        String offerDetails,

        @Size(
                max = 500,
                message = "Location cannot exceed 500 characters"
        )
        String location,

        @Size(
                max = 1000,
                message = "Banner URL cannot exceed 1000 characters"
        )
        String bannerImageUrl

) {
}