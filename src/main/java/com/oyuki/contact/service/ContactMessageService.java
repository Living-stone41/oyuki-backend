package com.oyuki.contact.service;

import com.oyuki.contact.dto.ContactMessageRequest;
import com.oyuki.contact.dto.ContactMessageResponse;
import com.oyuki.contact.entity.ContactMessage;
import com.oyuki.contact.enums.ContactMessageStatus;
import com.oyuki.contact.repository.ContactMessageRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ContactMessageService {

    private final ContactMessageRepository repository;
    private final SimpMessagingTemplate messagingTemplate;

    public ContactMessageService(
            ContactMessageRepository repository,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.repository = repository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public ContactMessageResponse create(
            ContactMessageRequest request
    ) {
        ContactMessage message = new ContactMessage();

        message.setFullName(request.getFullName().trim());
        message.setEmail(
                request.getEmail().trim().toLowerCase()
        );

        if (
                request.getPhoneNumber() != null &&
                !request.getPhoneNumber().isBlank()
        ) {
            message.setPhoneNumber(
                    request.getPhoneNumber().trim()
            );
        }

        message.setSubject(request.getSubject().trim());
        message.setMessage(request.getMessage().trim());
        message.setStatus(ContactMessageStatus.NEW);

        ContactMessage saved = repository.save(message);

        ContactMessageResponse response =
                ContactMessageResponse.fromEntity(saved);

        /*
         * Instantly notify every connected admin dashboard.
         */
        messagingTemplate.convertAndSend(
                "/topic/admin/contact-messages",
                response
        );

        /*
         * Instantly update the unread-message count.
         */
        messagingTemplate.convertAndSend(
                "/topic/admin/contact-message-count",
                repository.countByStatus(
                        ContactMessageStatus.NEW
                )
        );

        return response;
    }

    @Transactional(readOnly = true)
    public List<ContactMessageResponse> getAll(
            ContactMessageStatus status
    ) {
        List<ContactMessage> messages =
                status == null
                        ? repository.findAllByOrderByCreatedAtDesc()
                        : repository
                        .findByStatusOrderByCreatedAtDesc(status);

        return messages.stream()
                .map(ContactMessageResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public ContactMessageResponse getOne(Long id) {
        return ContactMessageResponse.fromEntity(
                findMessage(id)
        );
    }

    @Transactional
    public ContactMessageResponse updateStatus(
            Long id,
            ContactMessageStatus status
    ) {
        ContactMessage message = findMessage(id);

        message.setStatus(status);

        ContactMessage saved = repository.save(message);

        ContactMessageResponse response =
                ContactMessageResponse.fromEntity(saved);

        messagingTemplate.convertAndSend(
                "/topic/admin/contact-message-updates",
                response
        );

        messagingTemplate.convertAndSend(
                "/topic/admin/contact-message-count",
                repository.countByStatus(
                        ContactMessageStatus.NEW
                )
        );

        return response;
    }

    @Transactional
    public void delete(Long id) {
        ContactMessage message = findMessage(id);

        repository.delete(message);

        messagingTemplate.convertAndSend(
                "/topic/admin/contact-message-deleted",
                id
        );
    }

    @Transactional(readOnly = true)
    public long getNewMessageCount() {
        return repository.countByStatus(
                ContactMessageStatus.NEW
        );
    }

    private ContactMessage findMessage(Long id) {
        return repository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Contact message was not found."
                        )
                );
    }
}