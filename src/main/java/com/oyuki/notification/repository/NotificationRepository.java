package com.oyuki.notification.repository;

import com.oyuki.notification.entity.Notification;
import com.oyuki.notification.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository
        extends JpaRepository<Notification, Long> {

    List<Notification>
    findAllByRecipient_IdOrderByCreatedAtDesc(
            Long recipientId
    );

    List<Notification>
    findAllByRecipient_IdAndReadFalseOrderByCreatedAtDesc(
            Long recipientId
    );

    long countByRecipient_IdAndReadFalse(
            Long recipientId
    );

    Optional<Notification> findByIdAndRecipient_Id(
            Long notificationId,
            Long recipientId
    );

    List<Notification>
    findAllByRecipient_IdAndReferenceTypeAndReferenceIdOrderByCreatedAtDesc(
            Long recipientId,
            String referenceType,
            Long referenceId
    );

    boolean existsByRecipient_IdAndTypeAndReferenceTypeAndReferenceId(
            Long recipientId,
            NotificationType type,
            String referenceType,
            Long referenceId
    );

    long deleteAllByRecipient_Id(
            Long recipientId
    );
}