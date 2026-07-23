package com.oyuki.farmersday.entity;

import com.oyuki.farmersday.enums.FarmersDayStatus;
import com.oyuki.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(
        name = "farmers_day_events",
        indexes = {
                @Index(
                        name = "idx_farmers_day_event_date",
                        columnList = "event_date"
                ),
                @Index(
                        name = "idx_farmers_day_status",
                        columnList = "status"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FarmersDayEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "title",
            nullable = false,
            length = 200
    )
    private String title;

    @Column(
            name = "description",
            nullable = false,
            length = 4000
    )
    private String description;

    @Column(
            name = "event_date",
            nullable = false
    )
    private LocalDate eventDate;

    @Column(
            name = "start_time",
            nullable = false
    )
    private LocalTime startTime;

    @Column(
            name = "end_time",
            nullable = false
    )
    private LocalTime endTime;

    /*
     * Examples:
     *
     * Up to 30% discount on selected products.
     * Free delivery for orders above ₦20,000.
     */
    @Column(
            name = "offer_details",
            length = 2000
    )
    private String offerDetails;

    /*
     * Can contain a physical location or:
     *
     * "Oyuki Online Marketplace"
     */
    @Column(
            name = "location",
            length = 500
    )
    private String location;

    @Column(
            name = "banner_image_url",
            length = 1000
    )
    private String bannerImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 30
    )
    @Builder.Default
    private FarmersDayStatus status =
            FarmersDayStatus.DRAFT;

    /*
     * Admin who created the event.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "created_by_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_farmers_day_created_by"
            )
    )
    private User createdBy;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

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

        if (status == null) {
            status = FarmersDayStatus.DRAFT;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}