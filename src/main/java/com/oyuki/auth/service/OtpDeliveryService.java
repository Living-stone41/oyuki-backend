package com.oyuki.auth.service;

import com.oyuki.auth.enums.OtpChannel;
import com.oyuki.user.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class OtpDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(OtpDeliveryService.class);

    private final JavaMailSender mailSender;
    private final TwilioVerifyService twilioVerifyService;
    private final String senderEmail;

    public OtpDeliveryService(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            TwilioVerifyService twilioVerifyService,

            @Value("${app.mail.from:}")
            String senderEmail
    ) {
        this.mailSender =
                mailSenderProvider.getIfAvailable();

        this.twilioVerifyService =
                twilioVerifyService;

        this.senderEmail =
                senderEmail == null
                        ? ""
                        : senderEmail.trim();
    }

    /*
     * Registration OTP.
     *
     * EMAIL:
     * The backend-generated OTP is emailed.
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
     * Backward-compatible method.
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
     * Used when resending registration OTP to the
     * exact contact selected by the user.
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
     * Password reset OTP.
     *
     * EMAIL:
     * The locally generated OTP is sent.
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
     * Do not use this old method because password-reset
     * delivery must verify that the destination belongs
     * to the correct user.
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

    private OtpChannel resolveChannel(
            User user,
            OtpChannel requestedChannel
    ) {
        if (requestedChannel == OtpChannel.EMAIL) {
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

        if (requestedChannel == OtpChannel.PHONE) {
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

    private void sendEmail(
            String recipient,
            String subject,
            String body
    ) {
        if (
                recipient == null ||
                recipient.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "Recipient email is required"
            );
        }

        if (mailSender == null) {
            throw new IllegalStateException(
                    "Email OTP delivery is not configured. Check the Oyuki Gmail SMTP settings."
            );
        }

        if (senderEmail.isBlank()) {
            throw new IllegalStateException(
                    "The Oyuki sender email is not configured"
            );
        }

        SimpleMailMessage message =
                new SimpleMailMessage();

        message.setFrom(senderEmail);
        message.setTo(recipient);
        message.setSubject(subject);
        message.setText(body);

        try {
            mailSender.send(message);
            log.info("OTP email sent successfully to {}", maskEmail(recipient));

        } catch (Exception exception) {
            log.error(
                    "OTP email delivery failed for {}. Sender configured: {}",
                    maskEmail(recipient),
                    !senderEmail.isBlank(),
                    exception
            );

            Throwable root = rootCause(exception);
            throw new IllegalStateException(
                    "The OTP email could not be sent: "
                            + root.getClass().getSimpleName()
                            + " - "
                            + String.valueOf(root.getMessage()),
                    exception
            );
        }
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "unknown";
        }

        String[] parts = email.split("@", 2);
        String local = parts[0];
        String masked = local.length() <= 2
                ? "**"
                : local.substring(0, 2) + "***";

        return masked + "@" + parts[1];
    }

    private Throwable rootCause(Throwable exception) {
        Throwable current = exception;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
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
                        .equalsIgnoreCase(destination)
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
                        normalizePhone(destination)
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

        if (cleaned.startsWith("+234")) {
            return cleaned;
        }

        if (cleaned.startsWith("234")) {
            return "+" + cleaned;
        }

        if (cleaned.startsWith("0")) {
            return "+234"
                    + cleaned.substring(1);
        }

        if (cleaned.startsWith("+")) {
            return cleaned;
        }

        return "+234" + cleaned;
    }
}