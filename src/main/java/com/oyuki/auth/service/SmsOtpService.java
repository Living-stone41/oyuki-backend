package com.oyuki.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

@Service
public class SmsOtpService {

    private final boolean enabled;
    private final String accountSid;
    private final String authToken;
    private final String senderNumber;
    private final HttpClient httpClient;

    public SmsOtpService(
            @Value("${app.sms.enabled:false}") boolean enabled,
            @Value("${twilio.account-sid:}") String accountSid,
            @Value("${twilio.auth-token:}") String authToken,
            @Value("${twilio.phone-number:}") String senderNumber
    ) {
        this.enabled = enabled;
        this.accountSid = clean(accountSid);
        this.authToken = clean(authToken);
        this.senderNumber = clean(senderNumber);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public void sendRegistrationOtp(String phoneNumber, String otp) {
        send(
                phoneNumber,
                "Your Oyuki verification code is " + otp
                        + ". It expires in 10 minutes. Do not share this code."
        );
    }

    public void sendPasswordResetOtp(String phoneNumber, String otp) {
        send(
                phoneNumber,
                "Your Oyuki password reset code is " + otp
                        + ". It expires in 10 minutes. Do not share this code."
        );
    }

    private void send(String recipient, String messageBody) {
        if (!enabled) {
            throw new IllegalStateException(
                    "Phone OTP delivery is not configured. Enable the SMS provider or use an email address."
            );
        }

        validateConfiguration();

        String destination = formatNigerianPhone(recipient);
        String endpoint = "https://api.twilio.com/2010-04-01/Accounts/"
                + encodePath(accountSid)
                + "/Messages.json";

        String body = "To=" + encode(destination)
                + "&From=" + encode(senderNumber)
                + "&Body=" + encode(messageBody);

        String credentials = Base64.getEncoder().encodeToString(
                (accountSid + ":" + authToken).getBytes(StandardCharsets.UTF_8)
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Basic " + credentials)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(
                        "The SMS provider rejected the OTP request (status "
                                + response.statusCode() + ")."
                );
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("SMS delivery was interrupted", exception);
        } catch (Exception exception) {
            if (exception instanceof IllegalStateException stateException) {
                throw stateException;
            }
            throw new IllegalStateException(
                    "The OTP SMS could not be sent. Please try again.",
                    exception
            );
        }
    }

    private void validateConfiguration() {
        if (accountSid.isBlank() || authToken.isBlank() || senderNumber.isBlank()) {
            throw new IllegalStateException(
                    "Twilio SMS credentials are incomplete. Check TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN and TWILIO_PHONE_NUMBER."
            );
        }
    }

    private String formatNigerianPhone(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Phone number is required");
        }

        String phone = value.trim().replaceAll("[^0-9+]", "");

        if (phone.startsWith("+")) {
            return phone;
        }

        if (phone.startsWith("234")) {
            return "+" + phone;
        }

        if (phone.startsWith("0")) {
            return "+234" + phone.substring(1);
        }

        return "+234" + phone;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String encodePath(String value) {
        return value.replaceAll("[^A-Za-z0-9]", "");
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
