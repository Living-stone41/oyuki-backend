package com.oyuki.auth.service;

import com.oyuki.auth.enums.OtpChannel;
import com.oyuki.user.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class OtpDeliveryService {

    private static final Logger log =
            LoggerFactory.getLogger(
                    OtpDeliveryService.class
            );

    private static final String RESEND_EMAIL_URL =
            "https://api.resend.com/emails";

    private final TwilioVerifyService twilioVerifyService;
    private final HttpClient httpClient;
    private final String resendApiKey;
    private final String senderEmail;

    public OtpDeliveryService(
            TwilioVerifyService twilioVerifyService,

            @Value("${resend.api-key:}")
            String resendApiKey,

            @Value("${resend.from-email:}")
            String senderEmail
    ) {
        this.twilioVerifyService =
                twilioVerifyService;

        this.resendApiKey =
                resendApiKey == null
                        ? ""
                        : resendApiKey.trim();

        this.senderEmail =
                senderEmail == null
                        ? ""
                        : senderEmail.trim();

        this.httpClient =
                HttpClient.newBuilder()
                        .connectTimeout(
                                Duration.ofSeconds(20)
                        )
                        .build();
    }

    /*
     * Registration OTP.
     *
     * EMAIL:
     * The backend-generated OTP is sent through
     * the Resend HTTPS API.
     *
     * PHONE:
     * Twilio Verify generates and sends its own OTP.
     */
    public String sendRegistrationOtp(
            User user,
            String otp,
            OtpChannel requestedChannel
    ) {
        validateUser(user);

        OtpChannel channel =
                resolveChannel(
                        user,
                        requestedChannel
                );

        if (channel == OtpChannel.EMAIL) {
            sendEmail(
                    user.getEmail(),
                    "Verify your Oyuki account",
                    registrationEmail(
                            user.getFullName(),
                            otp
                    )
            );

            return user.getEmail();
        }

        twilioVerifyService.sendOtp(
                user.getPhoneNumber()
        );

        return user.getPhoneNumber();
    }

    /*
     * Backward-compatible registration method.
     */
    public String sendRegistrationOtp(
            User user,
            String otp
    ) {
        return sendRegistrationOtp(
                user,
                otp,
                null
        );
    }

    /*
     * Used when resending a registration OTP
     * to the exact contact selected by the user.
     */
    public String sendRegistrationOtpToContact(
            User user,
            String otp,
            String destination
    ) {
        validateUser(user);

        String cleanDestination =
                requireDestination(destination);

        if (isEmail(cleanDestination)) {
            validateEmailBelongsToUser(
                    user,
                    cleanDestination
            );

            sendEmail(
                    user.getEmail(),
                    "Verify your Oyuki account",
                    registrationEmail(
                            user.getFullName(),
                            otp
                    )
            );

            return user.getEmail();
        }

        validatePhoneBelongsToUser(
                user,
                cleanDestination
        );

        twilioVerifyService.sendOtp(
                user.getPhoneNumber()
        );

        return user.getPhoneNumber();
    }

    /*
     * Password-reset OTP.
     *
     * EMAIL:
     * The locally generated OTP is sent through
     * the Resend HTTPS API.
     *
     * PHONE:
     * Twilio Verify generates and sends its own OTP.
     */
    public void sendPasswordResetOtp(
            User user,
            String destination,
            String otp
    ) {
        validateUser(user);

        String cleanDestination =
                requireDestination(destination);

        if (isEmail(cleanDestination)) {
            validateEmailBelongsToUser(
                    user,
                    cleanDestination
            );

            sendEmail(
                    user.getEmail(),
                    "Reset your Oyuki password",
                    passwordResetEmail(
                            user.getFullName(),
                            otp
                    )
            );

            return;
        }

        validatePhoneBelongsToUser(
                user,
                cleanDestination
        );

        twilioVerifyService.sendOtp(
                user.getPhoneNumber()
        );
    }

    /*
     * Do not use this old method because
     * password-reset delivery must verify
     * that the destination belongs to the user.
     */
    public void sendPasswordResetOtp(
            String destination,
            String otp
    ) {
        throw new IllegalStateException(
                "Use sendPasswordResetOtp(User, String, String)"
        );
    }

    /*
     * Called when verifying a phone OTP.
     */
    public boolean verifyPhoneOtp(
            String phoneNumber,
            String submittedCode
    ) {
        if (
                phoneNumber == null ||
                phoneNumber.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "Phone number is required"
            );
        }

        if (
                submittedCode == null ||
                submittedCode.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "Verification code is required"
            );
        }

        return twilioVerifyService.verifyOtp(
                phoneNumber,
                submittedCode
        );
    }

    /*
     * Sends an email through the Resend HTTPS API.
     */
    private void sendEmail(
            String recipient,
            String subject,
            String body
    ) {
        validateEmailConfiguration(recipient);

        String requestBody =
                buildEmailRequestBody(
                        recipient,
                        subject,
                        body
                );

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(
                                URI.create(
                                        RESEND_EMAIL_URL
                                )
                        )
                        .timeout(
                                Duration.ofSeconds(30)
                        )
                        .header(
                                "Authorization",
                                "Bearer " + resendApiKey
                        )
                        .header(
                                "Content-Type",
                                "application/json"
                        )
                        .header(
                                "Accept",
                                "application/json"
                        )
                        .POST(
                                HttpRequest.BodyPublishers
                                        .ofString(
                                                requestBody
                                        )
                        )
                        .build();

        try {
            HttpResponse<String> response =
                    httpClient.send(
                            request,
                            HttpResponse.BodyHandlers
                                    .ofString()
                    );

            int statusCode =
                    response.statusCode();

            if (
                    statusCode < 200 ||
                    statusCode >= 300
            ) {
                log.error(
                        "Resend rejected OTP email for {}. HTTP status: {}. Response: {}",
                        maskEmail(recipient),
                        statusCode,
                        response.body()
                );

                throw new IllegalStateException(
                        buildResendErrorMessage(
                                statusCode,
                                response.body()
                        )
                );
            }

            log.info(
                    "OTP email sent successfully to {} through Resend",
                    maskEmail(recipient)
            );

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            log.error(
                    "OTP email delivery was interrupted for {}",
                    maskEmail(recipient),
                    exception
            );

            throw new IllegalStateException(
                    "The OTP email could not be sent because the request was interrupted.",
                    exception
            );

        } catch (IOException exception) {
            Throwable root =
                    rootCause(exception);

            log.error(
                    "OTP email delivery failed for {} through Resend",
                    maskEmail(recipient),
                    exception
            );

            throw new IllegalStateException(
                    "The OTP email could not be sent: "
                            + root.getClass()
                            .getSimpleName()
                            + " - "
                            + safeErrorMessage(
                            root.getMessage()
                    ),
                    exception
            );
        }
    }

    private void validateEmailConfiguration(
            String recipient
    ) {
        if (
                recipient == null ||
                recipient.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "Recipient email is required"
            );
        }

        if (resendApiKey.isBlank()) {
            throw new IllegalStateException(
                    "Resend email delivery is not configured. Add RESEND_API_KEY to Railway."
            );
        }

        if (senderEmail.isBlank()) {
            throw new IllegalStateException(
                    "The Oyuki sender email is not configured. Add RESEND_FROM_EMAIL to Railway."
            );
        }
    }

    private String buildEmailRequestBody(
            String recipient,
            String subject,
            String body
    ) {
        return """
                {
                  "from": "%s",
                  "to": ["%s"],
                  "subject": "%s",
                  "text": "%s"
                }
                """.formatted(
                escapeJson(senderEmail),
                escapeJson(recipient),
                escapeJson(subject),
                escapeJson(body)
        );
    }

    private String buildResendErrorMessage(
            int statusCode,
            String responseBody
    ) {
        String response =
                responseBody == null ||
                        responseBody.isBlank()
                        ? "No response body"
                        : responseBody;

        if (statusCode == 401) {
            return "The OTP email could not be sent because the Resend API key is invalid.";
        }

        if (statusCode == 403) {
            return "The OTP email could not be sent because the sender domain is not verified or the sender is not allowed.";
        }

        if (statusCode == 422) {
            return "The OTP email could not be sent because Resend rejected the email details: "
                    + response;
        }

        if (statusCode == 429) {
            return "The OTP email could not be sent because the email sending limit was reached. Try again shortly.";
        }

        return "The OTP email could not be sent. Resend returned HTTP "
                + statusCode
                + ": "
                + response;
    }

    private OtpChannel resolveChannel(
            User user,
            OtpChannel requestedChannel
    ) {
        if (
                requestedChannel ==
                        OtpChannel.EMAIL
        ) {
            if (
                    user.getEmail() == null ||
                    user.getEmail().isBlank()
            ) {
                throw new IllegalArgumentException(
                        "Enter an email address to receive the OTP by email"
                );
            }

            return OtpChannel.EMAIL;
        }

        if (
                requestedChannel ==
                        OtpChannel.PHONE
        ) {
            if (
                    user.getPhoneNumber() == null ||
                    user.getPhoneNumber().isBlank()
            ) {
                throw new IllegalArgumentException(
                        "Enter a phone number to receive the OTP by SMS"
                );
            }

            return OtpChannel.PHONE;
        }

        if (
                user.getEmail() != null &&
                !user.getEmail().isBlank()
        ) {
            return OtpChannel.EMAIL;
        }

        if (
                user.getPhoneNumber() != null &&
                !user.getPhoneNumber().isBlank()
        ) {
            return OtpChannel.PHONE;
        }

        throw new IllegalStateException(
                "The user does not have an email address or phone number"
        );
    }

    private String maskEmail(
            String email
    ) {
        if (
                email == null ||
                !email.contains("@")
        ) {
            return "unknown";
        }

        String[] parts =
                email.split("@", 2);

        String local =
                parts[0];

        String masked =
                local.length() <= 2
                        ? "**"
                        : local.substring(0, 2)
                        + "***";

        return masked + "@" + parts[1];
    }

    private Throwable rootCause(
            Throwable exception
    ) {
        Throwable current =
                exception;

        while (
                current.getCause() != null &&
                current.getCause() != current
        ) {
            current =
                    current.getCause();
        }

        return current;
    }

    private String registrationEmail(
            String fullName,
            String otp
    ) {
        validateLocalOtp(otp);

        return "Hello "
                + safeName(fullName)
                + ",\n\n"
                + "Your Oyuki account verification code is:\n\n"
                + otp
                + "\n\n"
                + "This code expires in 10 minutes. "
                + "Do not share it with anyone.\n\n"
                + "If you did not create this account, "
                + "ignore this email.\n\n"
                + "Oyuki Marketplace";
    }

    private String passwordResetEmail(
            String fullName,
            String otp
    ) {
        validateLocalOtp(otp);

        return "Hello "
                + safeName(fullName)
                + ",\n\n"
                + "Your Oyuki password reset code is:\n\n"
                + otp
                + "\n\n"
                + "This code expires in 10 minutes. "
                + "Do not share it with anyone.\n\n"
                + "If you did not request a password reset, "
                + "ignore this email.\n\n"
                + "Oyuki Marketplace";
    }

    private void validateUser(
            User user
    ) {
        if (user == null) {
            throw new IllegalArgumentException(
                    "User is required"
            );
        }
    }

    private void validateLocalOtp(
            String otp
    ) {
        if (
                otp == null ||
                otp.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "OTP is required"
            );
        }
    }

    private void validateEmailBelongsToUser(
            User user,
            String destination
    ) {
        if (
                user.getEmail() == null ||
                !user.getEmail()
                        .equalsIgnoreCase(
                                destination
                        )
        ) {
            throw new IllegalArgumentException(
                    "The supplied email address does not belong to this account"
            );
        }
    }

    private void validatePhoneBelongsToUser(
            User user,
            String destination
    ) {
        if (
                user.getPhoneNumber() == null ||
                !normalizePhone(
                        user.getPhoneNumber()
                ).equals(
                        normalizePhone(
                                destination
                        )
                )
        ) {
            throw new IllegalArgumentException(
                    "The supplied phone number does not belong to this account"
            );
        }
    }

    private String requireDestination(
            String destination
    ) {
        if (
                destination == null ||
                destination.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "Email or phone number is required"
            );
        }

        return destination.trim();
    }

    private String safeName(
            String fullName
    ) {
        return fullName == null ||
                fullName.isBlank()
                ? "there"
                : fullName.trim();
    }

    private boolean isEmail(
            String value
    ) {
        return value != null &&
                value.contains("@");
    }

    private String normalizePhone(
            String value
    ) {
        if (
                value == null ||
                value.isBlank()
        ) {
            return "";
        }

        String cleaned =
                value.replaceAll(
                        "[^0-9+]",
                        ""
                );

        if (
                cleaned.startsWith("+234")
        ) {
            return cleaned;
        }

        if (
                cleaned.startsWith("234")
        ) {
            return "+" + cleaned;
        }

        if (
                cleaned.startsWith("0")
        ) {
            return "+234"
                    + cleaned.substring(1);
        }

        if (
                cleaned.startsWith("+")
        ) {
            return cleaned;
        }

        return "+234" + cleaned;
    }

    private String escapeJson(
            String value
    ) {
        if (value == null) {
            return "";
        }

        StringBuilder escaped =
                new StringBuilder();

        for (
                int index = 0;
                index < value.length();
                index++
        ) {
            char character =
                    value.charAt(index);

            switch (character) {
                case '"' ->
                        escaped.append("\\\"");

                case '\\' ->
                        escaped.append("\\\\");

                case '\b' ->
                        escaped.append("\\b");

                case '\f' ->
                        escaped.append("\\f");

                case '\n' ->
                        escaped.append("\\n");

                case '\r' ->
                        escaped.append("\\r");

                case '\t' ->
                        escaped.append("\\t");

                default -> {
                    if (character < 32) {
                        escaped.append(
                                String.format(
                                        "\\u%04x",
                                        (int) character
                                )
                        );
                    } else {
                        escaped.append(
                                character
                        );
                    }
                }
            }
        }

        return escaped.toString();
    }

    private String safeErrorMessage(
            String message
    ) {
        return message == null ||
                message.isBlank()
                ? "Unknown connection error"
                : message;
    }
}