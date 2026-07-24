package com.oyuki.contact.dto;

import com.oyuki.contact.enums.ContactMessageStatus;
import jakarta.validation.constraints.NotNull;

public class UpdateContactMessageRequest {

    @NotNull(message = "Status is required")
    private ContactMessageStatus status;

    public UpdateContactMessageRequest() {
    }

    public ContactMessageStatus getStatus() {
        return status;
    }

    public void setStatus(
            ContactMessageStatus status
    ) {
        this.status = status;
    }
}