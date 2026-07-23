package com.oyuki.common.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class OtpGenerator {

    private static final int OTP_BOUND = 1_000_000;

    private final SecureRandom secureRandom = new SecureRandom();

    public String generateSixDigitOtp() {
        int number = secureRandom.nextInt(OTP_BOUND);

        return String.format("%06d", number);
    }
}