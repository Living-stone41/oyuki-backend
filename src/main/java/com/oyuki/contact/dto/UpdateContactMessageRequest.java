package com.oyuki.contact.dto;

import com.oyuki.contact.enums.ContactMessageStatus;
import jakarta.validation.constraints.Size;

public record UpdateContactMessageRequest(
        ContactMessageStatus status,

        @Size(max = 4000, message = "Reply must not exceed 4000 characters")
        String reply
) {
}
