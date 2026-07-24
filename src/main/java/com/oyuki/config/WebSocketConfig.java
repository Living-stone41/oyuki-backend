package com.oyuki.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

import java.util.Arrays;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig
        implements WebSocketMessageBrokerConfigurer {

    private final String[] allowedOriginPatterns;

    public WebSocketConfig(
            @Value(
                    "${app.cors.allowed-origins:"
                            + "http://localhost:8080,"
                            + "http://localhost:5500,"
                            + "http://127.0.0.1:5500,"
                            + "https://*.up.railway.app}"
            )
            String allowedOrigins
    ) {
        this.allowedOriginPatterns =
                Arrays.stream(allowedOrigins.split(","))
                        .map(String::trim)
                        .filter(value -> !value.isBlank())
                        .toArray(String[]::new);
    }

    @Override
    public void configureMessageBroker(
            MessageBrokerRegistry registry
    ) {
        registry.enableSimpleBroker("/topic", "/queue");

        registry.setApplicationDestinationPrefixes("/app");

        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(
            StompEndpointRegistry registry
    ) {
        registry
                .addEndpoint("/ws")
                .setAllowedOriginPatterns(
                        allowedOriginPatterns
                );

        registry
                .addEndpoint("/ws")
                .setAllowedOriginPatterns(
                        allowedOriginPatterns
                )
                .withSockJS();
    }
}