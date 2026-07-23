package com.oyuki.auth.controller;

import com.oyuki.auth.dto.*;
import com.oyuki.auth.service.AuthService;
import com.oyuki.auth.service.LoginService;
import com.oyuki.auth.service.PasswordResetService;
import com.oyuki.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final LoginService loginService;
    private final PasswordResetService passwordResetService;

    public AuthController(
            AuthService authService,
            LoginService loginService,
            PasswordResetService passwordResetService
    ) {
        this.authService = authService;
        this.loginService = loginService;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Map<String, Object>>> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        Map<String, Object> result =
                authService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Registration started. Enter the verification code sent to you.",
                                result
                        )
                );
    }

    @PostMapping("/resend-registration-otp")
    public ResponseEntity<ApiResponse<Map<String, Object>>> resendRegistrationOtp(
            @Valid @RequestBody ResendRegistrationOtpRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "A new verification code has been sent",
                        authService.resendRegistrationOtp(request)));
    }

    @PostMapping("/verify-registration")
    public ResponseEntity<ApiResponse<Map<String, Object>>>
    verifyRegistration(
            @Valid @RequestBody VerifyRegistrationRequest request
    ) {
        Map<String, Object> result =
                authService.verifyRegistration(request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Registration verified successfully",
                        result
                )
        );
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request
    ) {
        LoginResponse result =
                loginService.login(request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Login successful",
                        result
                )
        );
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Map<String, Object>>>
    forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request
    ) {
        Map<String, Object> result =
                passwordResetService.requestPasswordReset(request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "If an account exists with those details, a reset code has been sent.",
                        result
                )
        );
    }

    @PostMapping("/verify-reset-token")
    public ResponseEntity<ApiResponse<Map<String, Object>>>
    verifyResetToken(
            @Valid @RequestBody VerifyPasswordResetRequest request
    ) {
        Map<String, Object> result =
                passwordResetService.verifyResetToken(request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Password reset code verified",
                        result
                )
        );
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Map<String, Object>>>
    resetPassword(
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        Map<String, Object> result =
                passwordResetService.resetPassword(request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Password updated successfully",
                        result
                )
        );
    }
}