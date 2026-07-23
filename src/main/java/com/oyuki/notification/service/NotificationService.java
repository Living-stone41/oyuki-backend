package com.oyuki.notification.service;

import com.oyuki.notification.dto.NotificationResponse;
import com.oyuki.notification.entity.Notification;
import com.oyuki.notification.enums.NotificationType;
import com.oyuki.notification.repository.NotificationRepository;
import com.oyuki.user.entity.User;
import com.oyuki.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(
            NotificationRepository notificationRepository,
            UserRepository userRepository
    ) {
        this.notificationRepository =
                notificationRepository;

        this.userRepository =
                userRepository;
    }

    /*
     * CREATE A NOTIFICATION USING A USER ID.
     *
     * Other services such as OrderService,
     * LogisticsService and RiderService will
     * call this method.
     */
    @Transactional
    public NotificationResponse sendNotification(
            Long recipientId,
            NotificationType type,
            String title,
            String message,
            String referenceType,
            Long referenceId,
            String actionUrl,
            String imageUrl
    ) {
        User recipient = getUser(recipientId);

        Notification notification =
                createNotificationEntity(
                        recipient,
                        type,
                        title,
                        message,
                        referenceType,
                        referenceId,
                        actionUrl,
                        imageUrl
                );

        Notification savedNotification =
                notificationRepository.save(
                        notification
                );

        return NotificationResponse.from(
                savedNotification
        );
    }

    /*
     * CREATE A NOTIFICATION USING AN
     * EXISTING USER ENTITY.
     */
    @Transactional
    public NotificationResponse sendNotification(
            User recipient,
            NotificationType type,
            String title,
            String message,
            String referenceType,
            Long referenceId,
            String actionUrl,
            String imageUrl
    ) {
        if (recipient == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Notification recipient is required"
            );
        }

        Notification notification =
                createNotificationEntity(
                        recipient,
                        type,
                        title,
                        message,
                        referenceType,
                        referenceId,
                        actionUrl,
                        imageUrl
                );

        Notification savedNotification =
                notificationRepository.save(
                        notification
                );

        return NotificationResponse.from(
                savedNotification
        );
    }

    /*
     * VIEW ALL OR ONLY UNREAD NOTIFICATIONS.
     */
    @Transactional(readOnly = true)
    public List<NotificationResponse>
    getMyNotifications(
            Long userId,
            boolean unreadOnly
    ) {
        getUser(userId);

        List<Notification> notifications;

        if (unreadOnly) {
            notifications =
                    notificationRepository
                            .findAllByRecipient_IdAndReadFalseOrderByCreatedAtDesc(
                                    userId
                            );
        } else {
            notifications =
                    notificationRepository
                            .findAllByRecipient_IdOrderByCreatedAtDesc(
                                    userId
                            );
        }

        return notifications.stream()
                .map(NotificationResponse::from)
                .toList();
    }

    /*
     * NOTIFICATION BELL UNREAD COUNT.
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(
            Long userId
    ) {
        getUser(userId);

        return notificationRepository
                .countByRecipient_IdAndReadFalse(
                        userId
                );
    }

    /*
     * MARK ONE NOTIFICATION AS READ.
     */
    @Transactional
    public NotificationResponse markAsRead(
            Long userId,
            Long notificationId
    ) {
        Notification notification =
                getUserNotification(
                        userId,
                        notificationId
                );

        notification.markAsRead();

        Notification savedNotification =
                notificationRepository.save(
                        notification
                );

        return NotificationResponse.from(
                savedNotification
        );
    }

    /*
     * MARK ALL USER NOTIFICATIONS AS READ.
     */
    @Transactional
    public long markAllAsRead(
            Long userId
    ) {
        getUser(userId);

        List<Notification> unreadNotifications =
                notificationRepository
                        .findAllByRecipient_IdAndReadFalseOrderByCreatedAtDesc(
                                userId
                        );

        unreadNotifications.forEach(
                Notification::markAsRead
        );

        notificationRepository.saveAll(
                unreadNotifications
        );

        return unreadNotifications.size();
    }

    /*
     * DELETE ONE NOTIFICATION.
     */
    @Transactional
    public void deleteNotification(
            Long userId,
            Long notificationId
    ) {
        Notification notification =
                getUserNotification(
                        userId,
                        notificationId
                );

        notificationRepository.delete(
                notification
        );
    }

    /*
     * DELETE ALL NOTIFICATIONS BELONGING
     * TO THE LOGGED-IN USER.
     */
    @Transactional
    public long clearMyNotifications(
            Long userId
    ) {
        getUser(userId);

        return notificationRepository
                .deleteAllByRecipient_Id(
                        userId
                );
    }

    /*
     * CHECK WHETHER A PARTICULAR NOTIFICATION
     * HAS ALREADY BEEN SENT.
     *
     * This will prevent duplicate Farmers'
     * Day reminders later.
     */
    @Transactional(readOnly = true)
    public boolean notificationAlreadyExists(
            Long recipientId,
            NotificationType type,
            String referenceType,
            Long referenceId
    ) {
        return notificationRepository
                .existsByRecipient_IdAndTypeAndReferenceTypeAndReferenceId(
                        recipientId,
                        type,
                        referenceType,
                        referenceId
                );
    }

    private Notification createNotificationEntity(
            User recipient,
            NotificationType type,
            String title,
            String message,
            String referenceType,
            Long referenceId,
            String actionUrl,
            String imageUrl
    ) {
        if (type == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Notification type is required"
            );
        }

        if (
                title == null ||
                title.isBlank()
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Notification title is required"
            );
        }

        if (
                message == null ||
                message.isBlank()
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Notification message is required"
            );
        }

        return Notification.builder()
                .recipient(recipient)
                .type(type)
                .title(title.trim())
                .message(message.trim())
                .referenceType(
                        clean(referenceType)
                )
                .referenceId(referenceId)
                .actionUrl(
                        clean(actionUrl)
                )
                .imageUrl(
                        clean(imageUrl)
                )
                .read(false)
                .build();
    }

    private Notification getUserNotification(
            Long userId,
            Long notificationId
    ) {
        return notificationRepository
                .findByIdAndRecipient_Id(
                        notificationId,
                        userId
                )
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Notification not found"
                        )
                );
    }

    private User getUser(
            Long userId
    ) {
        return userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "User account not found"
                        )
                );
    }

    private String clean(
            String value
    ) {
        if (
                value == null ||
                value.isBlank()
        ) {
            return null;
        }

        return value.trim();
    }
}