package com.oyuki.notification.entity;

import com.oyuki.notification.enums.NotificationType;
import com.oyuki.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "notifications",
        indexes = {
                @Index(
                        name = "idx_notifications_recipient",
                        columnList = "recipient_id"
                ),
                @Index(
                        name = "idx_notifications_recipient_read",
                        columnList = "recipient_id, is_read"
                ),
                @Index(
                        name = "idx_notifications_type",
                        columnList = "notification_type"
                ),
                @Index(
                        name = "idx_notifications_created_at",
                        columnList = "created_at"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * User receiving the notification.
     *
     * For a Farmers' Day broadcast, the system
     * creates one notification for every active
     * customer.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "recipient_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_notifications_recipient"
            )
    )
    private User recipient;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "notification_type",
            nullable = false,
            length = 60
    )
    private NotificationType type;

    @Column(
            name = "title",
            nullable = false,
            length = 200
    )
    private String title;

    @Column(
            name = "message",
            nullable = false,
            length = 2000
    )
    private String message;

    /*
     * Used to connect the notification to
     * an order, delivery, product or event.
     *
     * Examples:
     * referenceType = ORDER
     * referenceId = 12
     */
    @Column(
            name = "reference_type",
            length = 50
    )
    private String referenceType;

    @Column(name = "reference_id")
    private Long referenceId;

    /*
     * Frontend location that should open when
     * the user clicks the notification.
     *
     * Example:
     * /customer/orders/12
     */
    @Column(
            name = "action_url",
            length = 500
    )
    private String actionUrl;

    /*
     * Optional image used for Farmers' Day
     * announcements or promotional notifications.
     */
    @Column(
            name = "image_url",
            length = 1000
    )
    private String imageUrl;

    @Column(
            name = "is_read",
            nullable = false
    )
    @Builder.Default
    private boolean read = false;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void markAsRead() {
        if (!read) {
            read = true;
            readAt = LocalDateTime.now();
        }
    }
}