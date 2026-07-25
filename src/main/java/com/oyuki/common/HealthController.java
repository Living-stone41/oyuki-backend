package com.oyuki.common;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/")
    public Map<String, String> home() {
        return Map.of(
                "status", "online",
                "application", "Oyuki Backend",
                "message", "Oyuki API is running"
        );
    }

    @GetMapping("/api/health")
    public Map<String, String> health() {
        return Map.of(
                "status", "healthy",
                "service", "oyuki-backend"
        );
    }
}