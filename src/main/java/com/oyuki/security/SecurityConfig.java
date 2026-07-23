package com.oyuki.security;


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

import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
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


                        // Public HTML frontend and static assets
                        .requestMatchers(
                                "/",
                                "/index.html",
                                "/*.html",
                                "/assets/**",
                                "/favicon.ico",
                                "/error"
                        )
                        .permitAll()

                        // Browser preflight requests
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

                        // Public newsletter subscription and live marketplace statistics
                        .requestMatchers(HttpMethod.POST, "/api/newsletter/subscribe", "/api/newsletter/unsubscribe")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/public/stats")
                        .permitAll()
                        .requestMatchers("/api/newsletter/admin/**")
                        .hasRole("ADMIN")

                        // Public Farmers' Day endpoints
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/farmers-day/public/**"
                        )
                        .permitAll()

                        // Public marketplace products and uploaded images
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/marketplace/products/**",
                                "/uploads/**"
                        )
                        .permitAll()

                        // Logged-in providers manage their own products here.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/products/**"
                        )
                        .hasAnyRole("SELLER", "KITCHEN", "ADMIN")

                        // Public seller and kitchen profiles
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/sellers/public/**",
                                "/api/kitchens/public/**"
                        )
                        .permitAll()

                        /*
                         * ADMIN PAYMENT ROUTES
                         *
                         * Admin can configure the platform bank
                         * account and review payment receipts.
                         */
                        .requestMatchers(
                                "/api/admin/payments/**"
                        )
                        .hasRole("ADMIN")

                        // Farmers' Day administration
                        .requestMatchers(
                                "/api/farmers-day/admin/**"
                        )
                        .hasRole("ADMIN")

                        // New marketplace management modules
                        .requestMatchers("/api/complaints/admin/**", "/api/export-requests/admin/**", "/api/kyc/admin/**", "/api/wallet/admin/**")
                        .hasRole("ADMIN")

                        .requestMatchers("/api/complaints/**", "/api/export-requests/**")
                        .hasRole("CUSTOMER")

                        .requestMatchers("/api/kyc/**", "/api/wallet/**")
                        .hasAnyRole("SELLER", "KITCHEN")

                        .requestMatchers("/api/chat/**")
                        .authenticated()

                        // Every other admin endpoint
                        .requestMatchers(
                                "/api/admin/**"
                        )
                        .hasRole("ADMIN")

                        // Logistics administration
                        .requestMatchers(
                                "/api/logistics/**"
                        )
                        .hasAnyRole(
                                "LOGISTICS_ADMIN",
                                "ADMIN"
                        )

                        // Rider endpoints
                        .requestMatchers(
                                "/api/rider/**"
                        )
                        .hasRole("RIDER")

                        // Notifications for every logged-in user
                        .requestMatchers(
                                "/api/notifications/**"
                        )
                        .authenticated()

                        /*
                         * CUSTOMER PAYMENT ROUTES
                         *
                         * Customer can view the platform account,
                         * upload receipts and view payment history.
                         */
                        .requestMatchers(
                                "/api/payments/**"
                        )
                        .hasRole("CUSTOMER")

                        // Customer cart
                        .requestMatchers(
                                "/api/cart/**"
                        )
                        .hasRole("CUSTOMER")
                        .requestMatchers("/api/wishlist/**")
.hasRole("CUSTOMER")
                        // Customer checkout
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/orders/checkout"
                        )
                        .hasRole("CUSTOMER")

                        // Customer order history and details
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

                        // Every remaining endpoint needs a valid JWT
                        .requestMatchers(
        "/api/admin/order-deliveries/**"
)
.hasRole("ADMIN")

.requestMatchers(
        "/api/rider/order-deliveries/**"
)
.hasRole("RIDER")

.requestMatchers(
        HttpMethod.GET,
        "/api/tracking/**"
)
.hasRole("CUSTOMER")
.requestMatchers(
        HttpMethod.GET,
        "/api/reviews/products/**",
        "/api/reviews/providers/**",
        "/api/reviews/riders/**"
)
.permitAll()

.requestMatchers("/api/reviews/**")
.hasRole("CUSTOMER")

.requestMatchers("/api/admin/reviews/**")
.hasRole("ADMIN")
.requestMatchers("/api/addresses/**")
.hasRole("CUSTOMER")
.requestMatchers("/api/provider/pickup-address/**")
.hasAnyRole("SELLER", "KITCHEN")
.requestMatchers("/api/delivery-fees/**")
.hasRole("CUSTOMER")
.requestMatchers("/api/coupons/**")
.hasRole("CUSTOMER")
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

        configuration.setAllowedOriginPatterns(
                List.of(
                        "http://localhost:*",
                        "http://127.0.0.1:*"
                )
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
                List.of(
                        "Authorization",
                        "Content-Type",
                        "Accept",
                        "Origin",
                        "X-Requested-With"
                )
        );

        configuration.setExposedHeaders(
                List.of(
                        "Content-Disposition",
                        "Content-Type"
                )
        );

        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                configuration
        );

        return source;
    }
}