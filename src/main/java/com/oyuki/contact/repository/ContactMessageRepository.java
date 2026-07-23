package com.oyuki.contact.repository;

import com.oyuki.contact.entity.ContactMessage;
import com.oyuki.contact.enums.ContactMessageStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContactMessageRepository extends JpaRepository<ContactMessage, Long> {
    List<ContactMessage> findAllByOrderByCreatedAtDesc();
    List<ContactMessage> findAllByStatusOrderByCreatedAtDesc(ContactMessageStatus status);
}
