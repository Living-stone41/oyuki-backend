package com.oyuki.notification.controller;

import com.oyuki.notification.dto.NotificationResponse;
import com.oyuki.notification.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(
            NotificationService notificationService
    ) {
        this.notificationService =
                notificationService;
    }

    /*
     * GET ALL NOTIFICATIONS:
     *
     * GET /api/notifications
     *
     * GET ONLY UNREAD:
     *
     * GET /api/notifications?unreadOnly=true
     */
    @GetMapping
    public ResponseEntity<List<NotificationResponse>>
    getMyNotifications(
            Authentication authentication,

            @RequestParam(
                    defaultValue = "false"
            )
            boolean unreadOnly
    ) {
        Long userId =
                getUserId(authentication);

        return ResponseEntity.ok(
                notificationService
                        .getMyNotifications(
                                userId,
                                unreadOnly
                        )
        );
    }

    /*
     * GET UNREAD NOTIFICATION COUNT.
     */
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>>
    getUnreadCount(
            Authentication authentication
    ) {
        Long userId =
                getUserId(authentication);

        long unreadCount =
                notificationService
                        .getUnreadCount(userId);

        return ResponseEntity.ok(
                Map.of(
                        "unreadCount",
                        unreadCount
                )
        );
    }

    /*
     * MARK ONE NOTIFICATION AS READ.
     */
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<NotificationResponse>
    markAsRead(
            Authentication authentication,
            @PathVariable Long notificationId
    ) {
        Long userId =
                getUserId(authentication);

        return ResponseEntity.ok(
                notificationService.markAsRead(
                        userId,
                        notificationId
                )
        );
    }

    /*
     * MARK ALL NOTIFICATIONS AS READ.
     */
    @PatchMapping("/read-all")
    public ResponseEntity<Map<String, Object>>
    markAllAsRead(
            Authentication authentication
    ) {
        Long userId =
                getUserId(authentication);

        long updatedCount =
                notificationService
                        .markAllAsRead(userId);

        return ResponseEntity.ok(
                Map.of(
                        "success",
                        true,
                        "message",
                        "All notifications marked as read",
                        "updatedCount",
                        updatedCount
                )
        );
    }

    /*
     * DELETE ONE NOTIFICATION.
     */
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Map<String, Object>>
    deleteNotification(
            Authentication authentication,
            @PathVariable Long notificationId
    ) {
        Long userId =
                getUserId(authentication);

        notificationService.deleteNotification(
                userId,
                notificationId
        );

        return ResponseEntity.ok(
                Map.of(
                        "success",
                        true,
                        "message",
                        "Notification deleted"
                )
        );
    }

    /*
     * DELETE ALL USER NOTIFICATIONS.
     */
    @DeleteMapping("/clear")
    public ResponseEntity<Map<String, Object>>
    clearNotifications(
            Authentication authentication
    ) {
        Long userId =
                getUserId(authentication);

        long deletedCount =
                notificationService
                        .clearMyNotifications(
                                userId
                        );

        return ResponseEntity.ok(
                Map.of(
                        "success",
                        true,
                        "message",
                        "Notifications cleared",
                        "deletedCount",
                        deletedCount
                )
        );
    }

    private Long getUserId(
            Authentication authentication
    ) {
        return (Long) authentication.getPrincipal();
    }
}