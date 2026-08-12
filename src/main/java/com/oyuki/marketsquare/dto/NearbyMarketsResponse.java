package com.oyuki.marketsquare.dto;

import java.util.List;

public record NearbyMarketsResponse(
        double latitude,
        double longitude,
        String detectedState,
        Long stateId,
        String detectedLga,
        Long lgaId,
        String detectionSource,
        List<MarketSummary> markets,
        String message
) {
    public record MarketSummary(
            Long id,
            String name,
            String address,
            String categories,
            Double latitude,
            Double longitude,
            Double distanceKm
    ) {
    }
}
