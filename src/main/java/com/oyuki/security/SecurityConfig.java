package com.oyuki.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
                 * Uses the CorsConfigurationSource bean
                 * defined inside CorsConfig.java.
                 */
                .cors(cors -> {
                })

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
                         * Public health and error endpoints.
                         */
                        .requestMatchers(
                                "/",
                                "/api/health",
                                "/error"
                        ).permitAll()

                        /*
                         * Authentication endpoints.
                         */
                        .requestMatchers(
                                "/api/auth/**"
                        ).permitAll()

                        /*
                         * Public marketplace endpoints.
                         */
                        .requestMatchers(
                                "/api/marketplace/**"
                        ).permitAll()

                        /*
                         * Public reviews.
                         */
                        .requestMatchers(
                                "/api/reviews/providers/**"
                        ).permitAll()

                        /*
                         * Newsletter subscription.
                         */
                        .requestMatchers(
                                "/api/newsletter/**"
                        ).permitAll()

                        /*
                         * Public uploaded product and profile images.
                         */
                        .requestMatchers(
                                "/uploads/**"
                        ).permitAll()

                        /*
                         * Administrator endpoints.
                         */
                        .requestMatchers(
                                "/api/admin/**"
                        ).hasRole("ADMIN")

                        /*
                         * Account officer endpoints.
                         */
                        .requestMatchers(
                                "/api/account-officer/**"
                        ).hasAnyRole(
                                "ACCOUNT_OFFICER",
                                "ADMIN"
                        )

                        /*
                         * Logistics administrator endpoints.
                         */
                        .requestMatchers(
                                "/api/logistics-admin/**"
                        ).hasAnyRole(
                                "LOGISTIC_ADMIN",
                                "LOGISTICS_ADMIN",
                                "ADMIN"
                        )

                        /*
                         * Rider endpoints.
                         */
                        .requestMatchers(
                                "/api/rider/**"
                        ).hasAnyRole(
                                "RIDER",
                                "ADMIN"
                        )

                        /*
                         * Every other endpoint requires login.
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