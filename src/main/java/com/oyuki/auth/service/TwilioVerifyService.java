package com.oyuki.auth.service;

import com.twilio.Twilio;
import com.twilio.rest.verify.v2.service.Verification;
import com.twilio.rest.verify.v2.service.VerificationCheck;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TwilioVerifyService {

    private final boolean smsEnabled;
    private final String accountSid;
    private final String authToken;
    private final String verifyServiceSid;

    public TwilioVerifyService(

            @Value("${app.sms.enabled:false}")
            boolean smsEnabled,

            @Value("${twilio.account-sid:}")
            String accountSid,

            @Value("${twilio.auth-token:}")
            String authToken,

            @Value("${twilio.verify-service-sid:}")
            String verifyServiceSid
    ) {
        this.smsEnabled = smsEnabled;
        this.accountSid = clean(accountSid);
        this.authToken = clean(authToken);
        this.verifyServiceSid =
                clean(verifyServiceSid);
    }

    public void sendOtp(
            String phoneNumber
    ) {
        validateConfiguration();

        String formattedPhone =
                formatNigerianPhone(
                        phoneNumber
                );

        Twilio.init(
                accountSid,
                authToken
        );

        Verification verification =
                Verification.creator(
                        verifyServiceSid,
                        formattedPhone,
                        "sms"
                ).create();

        if (
                verification.getStatus() == null
                        || verification
                        .getStatus()
                        .isBlank()
        ) {
            throw new IllegalStateException(
                    "Twilio did not accept the OTP request"
            );
        }

        System.out.println(
                "Twilio OTP status: "
                        + verification.getStatus()
        );
    }

    public boolean verifyOtp(
            String phoneNumber,
            String code
    ) {
        validateConfiguration();

        String formattedPhone =
                formatNigerianPhone(
                        phoneNumber
                );

        Twilio.init(
                accountSid,
                authToken
        );

        VerificationCheck result =
                VerificationCheck.creator(
                        verifyServiceSid
                )
                        .setTo(formattedPhone)
                        .setCode(code)
                        .create();

        return "approved".equalsIgnoreCase(
                result.getStatus()
        );
    }

    private void validateConfiguration() {
        if (!smsEnabled) {
            throw new IllegalStateException(
                    "SMS OTP is disabled"
            );
        }

        if (
                accountSid.isBlank()
                        || authToken.isBlank()
                        || verifyServiceSid.isBlank()
        ) {
            throw new IllegalStateException(
                    "Twilio Verify configuration is incomplete"
            );
        }
    }

    private String formatNigerianPhone(
            String phone
    ) {
        if (
                phone == null
                        || phone.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "Phone number is required"
            );
        }

        String cleaned =
                phone.replaceAll(
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

        return "+234" + cleaned;
    }

    private String clean(
            String value
    ) {
        return value == null
                ? ""
                : value.trim();
    }
}