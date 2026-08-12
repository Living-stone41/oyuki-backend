package com.oyuki.marketsquare.controller;

import com.oyuki.marketsquare.dto.NearbyMarketsResponse;
import com.oyuki.marketsquare.entity.LocalGovernment;
import com.oyuki.marketsquare.entity.Market;
import com.oyuki.marketsquare.entity.State;
import com.oyuki.marketsquare.repository.LocalGovernmentRepository;
import com.oyuki.marketsquare.repository.MarketRepository;
import com.oyuki.marketsquare.repository.StateRepository;
import com.oyuki.marketsquare.service.MarketLocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/market-directory")
@RequiredArgsConstructor
public class MarketDirectoryController {

    private final StateRepository states;
    private final LocalGovernmentRepository lgas;
    private final MarketRepository markets;
    private final MarketLocationService marketLocationService;

    @GetMapping("/states")
    public List<State> states() {
        return states.findAllByActiveTrueOrderByNameAsc();
    }

    @GetMapping("/lgas")
    public List<LocalGovernment> lgas(@RequestParam(required = false) Long stateId) {
        if (stateId == null) {
            return lgas.findAllByActiveTrueOrderByNameAsc();
        }
        return lgas.findAllByStateIdAndActiveTrueOrderByNameAsc(stateId);
    }

    @GetMapping("/markets")
    public List<Market> markets(
            @RequestParam(required = false) Long lgaId,
            @RequestParam(required = false) Long stateId
    ) {
        if (lgaId != null) {
            return markets.findAllByLgaIdAndActiveTrueOrderByNameAsc(lgaId);
        }
        if (stateId != null) {
            return markets.findAllByLgaStateIdAndActiveTrueOrderByNameAsc(stateId);
        }
        return markets.findAllByActiveTrueOrderByNameAsc();
    }

    @GetMapping("/nearby")
    public NearbyMarketsResponse nearby(
            @RequestParam double lat,
            @RequestParam double lng
    ) {
        return marketLocationService.findMarketsForCoordinates(lat, lng);
    }
}
