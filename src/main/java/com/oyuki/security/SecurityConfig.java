package com.oyuki.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
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
        this.jwtAuthenticationFilter =
                jwtAuthenticationFilter;

        this.authenticationProvider =
                authenticationProvider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                /*
                 * Spring Security automatically finds and uses
                 * the CorsConfigurationSource bean from CorsConfig.
                 */
                .cors(cors -> {
                })

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        // Authentication endpoints
                        .requestMatchers(
                                "/api/auth/**"
                        ).permitAll()

                        // Public marketplace endpoints
                        .requestMatchers(
                                "/api/marketplace/**"
                        ).permitAll()

                        // Public review endpoints
                        .requestMatchers(
                                "/api/reviews/providers/**"
                        ).permitAll()

                        // Newsletter subscription
                        .requestMatchers(
                                "/api/newsletter/**"
                        ).permitAll()

                        // Publicly accessible uploaded images
                        .requestMatchers(
                                "/uploads/**"
                        ).permitAll()

                        // Spring Boot error endpoint
                        .requestMatchers(
                                "/error"
                        ).permitAll()

                        // Browser preflight requests
                        .requestMatchers(
                                org.springframework.http.HttpMethod.OPTIONS,
                                "/**"
                        ).permitAll()

                        // Admin endpoints
                        .requestMatchers(
                                "/api/admin/**"
                        ).hasRole("ADMIN")

                        // Account officer endpoints
                        .requestMatchers(
                                "/api/account-officer/**"
                        ).hasAnyRole(
                                "ACCOUNT_OFFICER",
                                "ADMIN"
                        )

                        // Logistics administrator endpoints
                        .requestMatchers(
                                "/api/logistics-admin/**"
                        ).hasAnyRole(
                                "LOGISTIC_ADMIN",
                                "LOGISTICS_ADMIN",
                                "ADMIN"
                        )

                        // Rider endpoints
                        .requestMatchers(
                                "/api/rider/**"
                        ).hasAnyRole(
                                "RIDER",
                                "ADMIN"
                        )

                        // All remaining requests require authentication
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