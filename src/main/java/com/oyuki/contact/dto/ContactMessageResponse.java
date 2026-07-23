package com.oyuki.contact.dto;

import com.oyuki.contact.enums.ContactMessageStatus;

import java.time.LocalDateTime;

public record ContactMessageResponse(
        Long id,
        String name,
        String email,
        String subject,
        String message,
        ContactMessageStatus status,
        String adminReply,
        LocalDateTime repliedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
