package com.oyuki.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final List<String> allowedOriginPatterns;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            @Value(
                    "${app.cors.allowed-origins:"
                            + "http://localhost:8080,"
                            + "http://localhost:5500,"
                            + "http://127.0.0.1:5500,"
                            + "https://*.up.railway.app}"
            )
            String allowedOrigins
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;

        this.allowedOriginPatterns = Arrays.stream(
                        allowedOrigins.split(",")
                )
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toList();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                .cors(cors ->
                        cors.configurationSource(
                                corsConfigurationSource()
                        )
                )

                .csrf(csrf -> csrf.disable())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        // Public HTML pages and static files
                        .requestMatchers(
                                "/",
                                "/index.html",
                                "/*.html",
                                "/assets/**",
                                "/favicon.ico",
                                "/error"
                        )
                        .permitAll()

                        // Browser CORS preflight requests
                        .requestMatchers(
                                HttpMethod.OPTIONS,
                                "/**"
                        )
                        .permitAll()

                        // Registration, login, OTP and password reset
                        .requestMatchers("/api/auth/**")
                        .permitAll()

                        // Public contact form
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/contact"
                        )
                        .permitAll()

                        // Public newsletter endpoints
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/newsletter/subscribe",
                                "/api/newsletter/unsubscribe"
                        )
                        .permitAll()

                        // Public marketplace statistics
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/public/stats"
                        )
                        .permitAll()

                        // Newsletter administration
                        .requestMatchers(
                                "/api/newsletter/admin/**"
                        )
                        .hasRole("ADMIN")

                        // Public Farmers' Day endpoints
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/farmers-day/public/**"
                        )
                        .permitAll()

                        // Public marketplace products and uploaded files
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/marketplace/products/**",
                                "/uploads/**"
                        )
                        .permitAll()

                        // Public seller and kitchen profiles
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/sellers/public/**",
                                "/api/kitchens/public/**"
                        )
                        .permitAll()

                        // Public reviews
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/reviews/products/**",
                                "/api/reviews/providers/**",
                                "/api/reviews/riders/**"
                        )
                        .permitAll()

                        // Admin payment routes
                        .requestMatchers(
                                "/api/admin/payments/**"
                        )
                        .hasRole("ADMIN")

                        // Farmers' Day administration
                        .requestMatchers(
                                "/api/farmers-day/admin/**"
                        )
                        .hasRole("ADMIN")

                        // Admin management modules
                        .requestMatchers(
                                "/api/complaints/admin/**",
                                "/api/export-requests/admin/**",
                                "/api/kyc/admin/**",
                                "/api/wallet/admin/**"
                        )
                        .hasRole("ADMIN")

                        // Customer complaints and export requests
                        .requestMatchers(
                                "/api/complaints/**",
                                "/api/export-requests/**"
                        )
                        .hasRole("CUSTOMER")

                        // Provider KYC and wallet routes
                        .requestMatchers(
                                "/api/kyc/**",
                                "/api/wallet/**"
                        )
                        .hasAnyRole("SELLER", "KITCHEN")

                        // Chat requires login
                        .requestMatchers("/api/chat/**")
                        .authenticated()

                        // Admin delivery management
                        .requestMatchers(
                                "/api/admin/order-deliveries/**"
                        )
                        .hasRole("ADMIN")

                        // Admin review management
                        .requestMatchers(
                                "/api/admin/reviews/**"
                        )
                        .hasRole("ADMIN")

                        // Every other admin endpoint
                        .requestMatchers("/api/admin/**")
                        .hasRole("ADMIN")

                        // Logistics administration
                        .requestMatchers("/api/logistics/**")
                        .hasAnyRole(
                                "LOGISTICS_ADMIN",
                                "ADMIN"
                        )

                        // Rider endpoints
                        .requestMatchers("/api/rider/**")
                        .hasRole("RIDER")

                        // Rider delivery endpoints
                        .requestMatchers(
                                "/api/rider/order-deliveries/**"
                        )
                        .hasRole("RIDER")

                        // Notifications
                        .requestMatchers("/api/notifications/**")
                        .authenticated()

                        // Customer payments
                        .requestMatchers("/api/payments/**")
                        .hasRole("CUSTOMER")

                        // Customer cart
                        .requestMatchers("/api/cart/**")
                        .hasRole("CUSTOMER")

                        // Customer wishlist
                        .requestMatchers("/api/wishlist/**")
                        .hasRole("CUSTOMER")

                        // Customer checkout
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/orders/checkout"
                        )
                        .hasRole("CUSTOMER")

                        // Customer order history
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/orders/my",
                                "/api/orders/my/**"
                        )
                        .hasRole("CUSTOMER")

                        // Seller and kitchen order management
                        .requestMatchers(
                                "/api/orders/provider/**"
                        )
                        .hasAnyRole(
                                "SELLER",
                                "KITCHEN"
                        )

                        // Product viewing for providers and admin
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/products/**"
                        )
                        .hasAnyRole(
                                "SELLER",
                                "KITCHEN",
                                "ADMIN"
                        )

                        // Product creation
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/products/**"
                        )
                        .hasAnyRole(
                                "SELLER",
                                "KITCHEN",
                                "ADMIN"
                        )

                        // Product updates
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/products/**"
                        )
                        .hasAnyRole(
                                "SELLER",
                                "KITCHEN",
                                "ADMIN"
                        )

                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/products/**"
                        )
                        .hasAnyRole(
                                "SELLER",
                                "KITCHEN",
                                "ADMIN"
                        )

                        // Product deletion
                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/products/**"
                        )
                        .hasAnyRole(
                                "SELLER",
                                "KITCHEN",
                                "ADMIN"
                        )

                        // Customer tracking
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/tracking/**"
                        )
                        .hasRole("CUSTOMER")

                        // Customer reviews
                        .requestMatchers("/api/reviews/**")
                        .hasRole("CUSTOMER")

                        // Customer addresses
                        .requestMatchers("/api/addresses/**")
                        .hasRole("CUSTOMER")

                        // Provider pickup address
                        .requestMatchers(
                                "/api/provider/pickup-address/**"
                        )
                        .hasAnyRole(
                                "SELLER",
                                "KITCHEN"
                        )

                        // Delivery fees
                        .requestMatchers("/api/delivery-fees/**")
                        .hasRole("CUSTOMER")

                        // Coupons
                        .requestMatchers("/api/coupons/**")
                        .hasRole("CUSTOMER")

                        // Everything else requires authentication
                        .anyRequest()
                        .authenticated()
                )

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration
    ) throws Exception {

        return configuration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration =
                new CorsConfiguration();

        /*
         * Allows exact domains and patterns such as:
         * https://example.up.railway.app
         * https://*.up.railway.app
         */
        configuration.setAllowedOriginPatterns(
                allowedOriginPatterns
        );

        configuration.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "PATCH",
                        "DELETE",
                        "OPTIONS"
                )
        );

        configuration.setAllowedHeaders(
                List.of("*")
        );

        configuration.setExposedHeaders(
                List.of(
                        "Authorization",
                        "Content-Disposition",
                        "Content-Type"
                )
        );

        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                configuration
        );

        return source;
    }
}