package com.oyuki.notification.service;

import com.resend.Resend;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final Resend resend;
    private final String fromEmail;

    public EmailService(
            @Value("${resend.api-key}") String apiKey,
            @Value("${resend.from-email}") String fromEmail
    ) {
        this.resend = new Resend(apiKey);
        this.fromEmail = fromEmail;
    }

    public void sendOtpEmail(
            String recipient,
            String fullName,
            String otp
    ) {
        String safeName =
                fullName == null || fullName.isBlank()
                        ? "there"
                        : fullName;

        String html = """
                <!DOCTYPE html>
                <html>
                <body style="font-family:Arial,sans-serif;background:#f5f7f6;padding:30px">
                    <div style="max-width:560px;margin:auto;background:white;padding:30px;border-radius:12px">
                        <h2 style="color:#198754">Oyuki verification code</h2>

                        <p>Hello %s,</p>

                        <p>Use the verification code below to continue:</p>

                        <div style="
                            font-size:32px;
                            font-weight:bold;
                            letter-spacing:8px;
                            color:#198754;
                            margin:24px 0;
                        ">
                            %s
                        </div>

                        <p>This code will expire shortly.</p>

                        <p>
                            Do not share this code with anyone.
                        </p>
                    </div>
                </body>
                </html>
                """.formatted(
                escapeHtml(safeName),
                escapeHtml(otp)
        );

        sendEmail(
                recipient,
                "Your Oyuki verification code",
                html
        );
    }

    public void sendPasswordResetEmail(
            String recipient,
            String fullName,
            String otp
    ) {
        String safeName =
                fullName == null || fullName.isBlank()
                        ? "there"
                        : fullName;

        String html = """
                <!DOCTYPE html>
                <html>
                <body style="font-family:Arial,sans-serif;background:#f5f7f6;padding:30px">
                    <div style="max-width:560px;margin:auto;background:white;padding:30px;border-radius:12px">
                        <h2 style="color:#198754">Reset your Oyuki password</h2>

                        <p>Hello %s,</p>

                        <p>Use this code to reset your password:</p>

                        <div style="
                            font-size:32px;
                            font-weight:bold;
                            letter-spacing:8px;
                            color:#198754;
                            margin:24px 0;
                        ">
                            %s
                        </div>

                        <p>
                            Ignore this email if you did not request a password reset.
                        </p>
                    </div>
                </body>
                </html>
                """.formatted(
                escapeHtml(safeName),
                escapeHtml(otp)
        );

        sendEmail(
                recipient,
                "Reset your Oyuki password",
                html
        );
    }

    public void sendEmail(
            String recipient,
            String subject,
            String html
    ) {
        try {
            CreateEmailOptions options =
                    CreateEmailOptions.builder()
                            .from(fromEmail)
                            .to(recipient)
                            .subject(subject)
                            .html(html)
                            .build();

            CreateEmailResponse response =
                    resend.emails().send(options);

            if (
                    response == null ||
                    response.getId() == null ||
                    response.getId().isBlank()
            ) {
                throw new IllegalStateException(
                        "Email provider did not return a message ID."
                );
            }
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "The OTP email could not be sent: "
                            + exception.getClass().getSimpleName()
                            + " - "
                            + exception.getMessage(),
                    exception
            );
        }
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}