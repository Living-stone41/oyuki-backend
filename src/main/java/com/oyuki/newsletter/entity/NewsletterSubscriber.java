package com.oyuki.newsletter.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "newsletter_subscribers", uniqueConstraints = @UniqueConstraint(columnNames = "email"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NewsletterSubscriber {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 190)
    private String email;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    private LocalDateTime subscribedAt;
    private LocalDateTime updatedAt;

    @PrePersist
    void create() { subscribedAt = updatedAt = LocalDateTime.now(); }

    @PreUpdate
    void update() { updatedAt = LocalDateTime.now(); }
}
