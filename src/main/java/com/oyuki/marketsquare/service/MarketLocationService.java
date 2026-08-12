package com.oyuki.marketsquare.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oyuki.marketsquare.dto.NearbyMarketsResponse;
import com.oyuki.marketsquare.entity.LocalGovernment;
import com.oyuki.marketsquare.entity.Market;
import com.oyuki.marketsquare.repository.LocalGovernmentRepository;
import com.oyuki.marketsquare.repository.MarketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class MarketLocationService {

    private final LocalGovernmentRepository lgaRepository;
    private final MarketRepository marketRepository;
    private final ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(6))
            .build();

    private final Map<String, DetectedArea> geocodeCache = new ConcurrentHashMap<>();
    private volatile long lastGeocodeRequestAt = 0L;

    @Value("${oyuki.geocoding.reverse-url:https://nominatim.openstreetmap.org/reverse}")
    private String reverseUrl;

    @Value("${oyuki.geocoding.user-agent:OyukiMarketplace/1.0 (support@oyukimarketplace.com)}")
    private String userAgent;

    @Value("${oyuki.geocoding.enabled:true}")
    private boolean geocodingEnabled;

    @Transactional(readOnly = true)
    public NearbyMarketsResponse findMarketsForCoordinates(double latitude, double longitude) {
        validateCoordinates(latitude, longitude);

        Optional<DetectedArea> geocoded = geocodingEnabled
                ? reverseGeocode(latitude, longitude)
                : Optional.empty();

        Optional<LocalGovernment> matchedLga = geocoded
                .flatMap(area -> matchLga(area.lgaCandidates(), area.stateName()));

        String source = "REVERSE_GEOCODING";

        if (matchedLga.isEmpty()) {
            matchedLga = nearestMarketLga(latitude, longitude);
            source = "NEAREST_MARKET";
        }

        if (matchedLga.isEmpty()) {
            String stateName = geocoded.map(DetectedArea::stateName).orElse(null);
            return new NearbyMarketsResponse(
                    latitude,
                    longitude,
                    stateName,
                    null,
                    null,
                    null,
                    "UNRESOLVED",
                    List.of(),
                    "We could not determine your LGA automatically. Please select your State and LGA manually."
            );
        }

        LocalGovernment lga = matchedLga.get();
        List<Market> markets = marketRepository.findAllByLgaIdAndActiveTrueOrderByNameAsc(lga.getId());

        List<NearbyMarketsResponse.MarketSummary> summaries = markets.stream()
                .map(market -> toSummary(market, latitude, longitude))
                .sorted(Comparator.comparing(
                        NearbyMarketsResponse.MarketSummary::distanceKm,
                        Comparator.nullsLast(Double::compareTo)
                ))
                .toList();

        String stateName = lga.getState() == null ? null : lga.getState().getName();
        String message = summaries.isEmpty()
                ? "Your location was detected as " + lga.getName() + ", but no active markets have been added for this LGA yet."
                : "Showing all active Oyuki markets in " + lga.getName() + ".";

        return new NearbyMarketsResponse(
                latitude,
                longitude,
                stateName,
                lga.getState() == null ? null : lga.getState().getId(),
                lga.getName(),
                lga.getId(),
                source,
                summaries,
                message
        );
    }

    private Optional<DetectedArea> reverseGeocode(double latitude, double longitude) {
        String cacheKey = String.format(Locale.ROOT, "%.4f,%.4f", latitude, longitude);
        DetectedArea cached = geocodeCache.get(cacheKey);
        if (cached != null) return Optional.of(cached);

        try {
            throttlePublicGeocoder();
            String url = reverseUrl
                    + "?format=jsonv2&addressdetails=1&zoom=12"
                    + "&lat=" + URLEncoder.encode(String.valueOf(latitude), StandardCharsets.UTF_8)
                    + "&lon=" + URLEncoder.encode(String.valueOf(longitude), StandardCharsets.UTF_8);

            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(8))
                    .header("User-Agent", userAgent)
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return Optional.empty();
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode address = root.path("address");

            String state = firstText(address, "state", "region");
            Set<String> candidates = new LinkedHashSet<>();

            addCandidate(candidates, address, "county");
            addCandidate(candidates, address, "municipality");
            addCandidate(candidates, address, "city_district");
            addCandidate(candidates, address, "borough");
            addCandidate(candidates, address, "city");
            addCandidate(candidates, address, "town");
            addCandidate(candidates, address, "suburb");

            DetectedArea detected = new DetectedArea(state, new ArrayList<>(candidates));
            geocodeCache.put(cacheKey, detected);
            return Optional.of(detected);
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }


    private synchronized void throttlePublicGeocoder() throws InterruptedException {
        long now = System.currentTimeMillis();
        long waitMs = 1100L - (now - lastGeocodeRequestAt);
        if (waitMs > 0) Thread.sleep(waitMs);
        lastGeocodeRequestAt = System.currentTimeMillis();
    }

    private Optional<LocalGovernment> matchLga(List<String> candidates, String stateName) {
        List<LocalGovernment> lgas = lgaRepository.findAllByActiveTrueOrderByNameAsc();
        String normalizedState = normalize(stateName);

        return lgas.stream()
                .filter(lga -> normalizedState.isBlank()
                        || lga.getState() == null
                        || normalize(lga.getState().getName()).equals(normalizedState)
                        || normalizedState.contains(normalize(lga.getState().getName())))
                .filter(lga -> candidates.stream().anyMatch(candidate -> {
                    String a = normalize(candidate);
                    String b = normalize(lga.getName());
                    return a.equals(b) || a.contains(b) || b.contains(a);
                }))
                .findFirst();
    }

    private Optional<LocalGovernment> nearestMarketLga(double latitude, double longitude) {
        return marketRepository.findAllByActiveTrueOrderByNameAsc().stream()
                .filter(m -> m.getLatitude() != null && m.getLongitude() != null && m.getLga() != null)
                .map(m -> new MarketDistance(m, distanceKm(latitude, longitude, m.getLatitude(), m.getLongitude())))
                .filter(x -> x.distanceKm() <= 35.0)
                .min(Comparator.comparingDouble(MarketDistance::distanceKm))
                .map(x -> x.market().getLga());
    }

    private NearbyMarketsResponse.MarketSummary toSummary(Market market, double latitude, double longitude) {
        Double distance = null;
        if (market.getLatitude() != null && market.getLongitude() != null) {
            distance = round2(distanceKm(latitude, longitude, market.getLatitude(), market.getLongitude()));
        }

        return new NearbyMarketsResponse.MarketSummary(
                market.getId(),
                market.getName(),
                market.getAddress(),
                market.getCategories(),
                market.getLatitude() == null ? null : market.getLatitude().doubleValue(),
                market.getLongitude() == null ? null : market.getLongitude().doubleValue(),
                distance
        );
    }

    private double distanceKm(double lat1, double lon1, BigDecimal lat2, BigDecimal lon2) {
        return distanceKm(lat1, lon1, lat2.doubleValue(), lon2.doubleValue());
    }

    private double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double earthRadiusKm = 6371.0088;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadiusKm * c;
    }

    private void validateCoordinates(double latitude, double longitude) {
        if (latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90");
        }
        if (longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180");
        }
    }

    private void addCandidate(Set<String> values, JsonNode address, String field) {
        String value = address.path(field).asText("").trim();
        if (!value.isBlank()) values.add(value);
    }

    private String firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            String value = node.path(field).asText("").trim();
            if (!value.isBlank()) return value;
        }
        return null;
    }

    private String normalize(String value) {
        if (value == null) return "";
        return value.toLowerCase(Locale.ROOT)
                .replace("local government area", "")
                .replace("local government", "")
                .replace(" lga", "")
                .replace("state", "")
                .replaceAll("[^a-z0-9]", "")
                .trim();
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private record DetectedArea(String stateName, List<String> lgaCandidates) {
    }

    private record MarketDistance(Market market, double distanceKm) {
    }
}
