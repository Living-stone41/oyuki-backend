package com.oyuki.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AuthenticationProvider authenticationProvider;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            AuthenticationProvider authenticationProvider
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authenticationProvider = authenticationProvider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .cors(Customizer.withDefaults())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        /*
                         * Allow browser CORS preflight requests.
                         */
                        .requestMatchers(
                                HttpMethod.OPTIONS,
                                "/**"
                        ).permitAll()

                        /*
                         * Public website pages and assets.
                         */
                        .requestMatchers(
                                "/",
                                "/index.html",
                                "/home.html",
                                "/about.html",
                                "/contact.html",
                                "/shop.html",
                                "/meals.html",
                                "/kitchens.html",
                                "/kitchen-detail.html",
                                "/product.html",
                                "/login.html",
                                "/register.html",
                                "/verify-otp.html",
                                "/forgot-password.html",
                                "/reset-password.html",
                                "/privacy.html",
                                "/terms.html",
                                "/feature-center.html",
                                "/markets.html",
                                "/favicon.ico",
                                "/assets/**"
                        ).permitAll()

                        /*
                         * Admin frontend pages and assets.
                         *
                         * These files can load publicly, but all admin
                         * information remains protected by /api/admin/**.
                         */
                        .requestMatchers(
                                "/admin",
                                "/admin/",
                                "/admin/index.html",
                                "/admin/admin-login.html",
                                "/admin/admin.html",
                                "/admin/assets/**"
                        ).permitAll()

                        /*
                         * Railway health and Spring error routes.
                         */
                        .requestMatchers(
                                "/api/health",
                                "/error"
                        ).permitAll()

                        /*
                         * Public authentication routes.
                         */
                        .requestMatchers(
                                "/api/auth/**"
                        ).permitAll()

                        /*
                         * Public marketplace routes.
                         */
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/marketplace/**"
                        ).permitAll()

                        /*
                         * Public statistics routes.
                         */
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/public/**"
                        ).permitAll()

                        /*
                         * Public provider reviews.
                         */
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/reviews/providers/**"
                        ).permitAll()

                        /*
                         * Public newsletter subscription.
                         */
                        .requestMatchers(
                                "/api/newsletter/**"
                        ).permitAll()

                        /*
                         * Public uploaded images and files.
                         */
                        .requestMatchers(
                                "/uploads/**"
                        ).permitAll()

                        /*
                         * Public Market Square directory and location lookup.
                         */
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/market-directory/**"
                        ).permitAll()

                        /*
                         * Administrator API routes.
                         */
                        .requestMatchers(
                                "/api/admin/**"
                        ).hasRole("ADMIN")

                        /*
                         * Account officer routes.
                         */
                        .requestMatchers(
                                "/api/account-officer/**"
                        ).hasAnyRole(
                                "ACCOUNT_OFFICER",
                                "ADMIN"
                        )

                        /*
                         * Logistics administrator routes.
                         */
                        .requestMatchers(
                                "/api/logistics-admin/**",
                                "/api/logistics/**"
                        ).hasAnyRole(
                                "LOGISTIC_ADMIN",
                                "LOGISTICS_ADMIN",
                                "ADMIN"
                        )

                        /*
                         * Rider routes.
                         */
                        .requestMatchers(
                                "/api/rider/**"
                        ).hasAnyRole(
                                "RIDER",
                                "ADMIN"
                        )

                        /* Market Square agent operations. */
                        .requestMatchers("/api/market-agent/**").hasAnyRole("MARKET_AGENT", "MARKET_SUPERVISOR", "ADMIN")

                        /*
                         * Seller and shared provider routes.
                         */
                        .requestMatchers(
                                "/api/seller/**",
                                "/api/provider/**"
                        ).hasAnyRole(
                                "SELLER",
                                "FARMER",
                                "KITCHEN",
                                "ADMIN"
                        )

                        /*
                         * Kitchen routes.
                         */
                        .requestMatchers(
                                "/api/kitchen/**"
                        ).hasAnyRole(
                                "KITCHEN",
                                "ADMIN"
                        )

                        /*
                         * Product management routes.
                         */
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/products/**"
                        ).hasAnyRole(
                                "SELLER",
                                "FARMER",
                                "KITCHEN",
                                "ADMIN"
                        )

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/products/**"
                        ).hasAnyRole(
                                "SELLER",
                                "FARMER",
                                "KITCHEN",
                                "ADMIN"
                        )

                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/products/**"
                        ).hasAnyRole(
                                "SELLER",
                                "FARMER",
                                "KITCHEN",
                                "ADMIN"
                        )

                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/products/**"
                        ).hasAnyRole(
                                "SELLER",
                                "FARMER",
                                "KITCHEN",
                                "ADMIN"
                        )

                        /*
                         * Viewing provider-owned products requires login.
                         */
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/products/**"
                        ).hasAnyRole(
                                "SELLER",
                                "FARMER",
                                "KITCHEN",
                                "ADMIN"
                        )

                        /*
                         * Any remaining API route requires authentication.
                         */
                        .requestMatchers(
                                "/api/**"
                        ).authenticated()

                        /*
                         * Any remaining request also requires authentication.
                         */
                        .anyRequest()
                        .authenticated()
                )

                .authenticationProvider(
                        authenticationProvider
                )

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}