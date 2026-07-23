package com.oyuki.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class MailConfigurationChecker implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(MailConfigurationChecker.class);

    @Value("${spring.mail.username:}")
    private String username;

    @Value("${spring.mail.password:}")
    private String password;

    @Override
    public void run(String... args) {
        String safeUsername = username == null || username.isBlank()
                ? "missing"
                : maskEmail(username.trim());

        int rawLength = password == null ? 0 : password.length();
        int compactLength = password == null ? 0 : password.replaceAll("\\s", "").length();

        log.info("Gmail username configured: {} ({})", username != null && !username.isBlank(), safeUsername);
        log.info("Gmail App Password configured: {}", password != null && !password.isBlank());
        log.info("Gmail App Password length: raw={}, without-spaces={}", rawLength, compactLength);

        if (password != null && !password.isBlank() && compactLength != 16) {
            log.warn("The Gmail App Password should normally contain 16 characters without spaces.");
        }
    }

    private String maskEmail(String email) {
        if (!email.contains("@")) {
            return "configured";
        }
        String[] parts = email.split("@", 2);
        String local = parts[0];
        return (local.length() <= 2 ? "**" : local.substring(0, 2) + "***") + "@" + parts[1];
    }
}
