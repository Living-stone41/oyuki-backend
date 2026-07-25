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
                         * Allow all browser CORS preflight requests.
                         */
                        .requestMatchers(
                                HttpMethod.OPTIONS,
                                "/**"
                        ).permitAll()

                        /*
                         * Public Railway health routes.
                         */
                        .requestMatchers(
                                "/",
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
                         * Administrator routes.
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

                        /*
                         * Seller and farmer routes.
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
                         * Kitchen profile routes.
                         */
                        .requestMatchers(
                                "/api/kitchen/**"
                        ).hasAnyRole(
                                "KITCHEN",
                                "ADMIN"
                        )

                        /*
                         * Every other route requires authentication.
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