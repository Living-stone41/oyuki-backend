package com.oyuki.config;

import com.oyuki.user.entity.User;
import com.oyuki.user.enums.AccountStatus;
import com.oyuki.user.enums.Role;
import com.oyuki.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminInitializer {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    @Bean
    CommandLineRunner createBootstrapAdmin(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.bootstrap.email:}") String adminEmail,
            @Value("${app.admin.bootstrap.password:}") String adminPassword,
            @Value("${app.admin.bootstrap.full-name:Oyuki Administrator}") String adminName
    ) {
        return args -> {
            String normalizedEmail = adminEmail == null ? "" : adminEmail.trim().toLowerCase();

            if (normalizedEmail.isBlank() || adminPassword == null || adminPassword.isBlank()) {
                log.info("Bootstrap admin skipped because ADMIN_EMAIL or ADMIN_PASSWORD is missing.");
                return;
            }

            if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
                log.info("Bootstrap admin already exists.");
                return;
            }

            User admin = User.builder()
                    .fullName(adminName)
                    .email(normalizedEmail)
                    .passwordHash(passwordEncoder.encode(adminPassword))
                    .role(Role.ADMIN)
                    .status(AccountStatus.ACTIVE)
                    .emailVerified(true)
                    .phoneVerified(false)
                    .tokenVersion(0)
                    .build();

            userRepository.save(admin);
            log.info("Bootstrap admin created successfully.");
        };
    }
}
