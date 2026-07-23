package com.oyuki.admin.dto;

import com.oyuki.user.enums.AccountStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateUserStatusRequest(

        @NotNull(message = "Account status is required")
        AccountStatus status,

        @Size(
                max = 500,
                message = "Status reason cannot exceed 500 characters"
        )
        String reason

) {
}
