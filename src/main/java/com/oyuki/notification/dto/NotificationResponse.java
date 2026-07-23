package com.oyuki.notification.dto;

import com.oyuki.notification.entity.Notification;
import com.oyuki.notification.enums.NotificationType;

import java.time.LocalDateTime;

public record NotificationResponse(

        Long id,
        NotificationType type,
        String title,
        String message,

        String referenceType,
        Long referenceId,

        String actionUrl,
        String imageUrl,

        boolean read,
        LocalDateTime readAt,
        LocalDateTime createdAt

) {

    public static NotificationResponse from(
            Notification notification
    ) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),

                notification.getReferenceType(),
                notification.getReferenceId(),

                notification.getActionUrl(),
                notification.getImageUrl(),

                notification.isRead(),
                notification.getReadAt(),
                notification.getCreatedAt()
        );
    }
}