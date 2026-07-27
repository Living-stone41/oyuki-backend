package com.oyuki.kitchen.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "kitchen_images", indexes = @Index(name = "idx_kitchen_images_profile", columnList = "kitchen_profile_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class KitchenImage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "kitchen_profile_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_kitchen_image_profile"))
    private KitchenProfile kitchenProfile;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "caption", length = 200)
    private String caption;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        if (displayOrder == null) displayOrder = 0;
    }
}
