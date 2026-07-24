package com.oyuki.contact.service;

import com.oyuki.contact.dto.ContactMessageRequest;
import com.oyuki.contact.dto.ContactMessageResponse;
import com.oyuki.contact.dto.UpdateContactMessageRequest;
import com.oyuki.contact.entity.ContactMessage;
import com.oyuki.contact.enums.ContactMessageStatus;
import com.oyuki.contact.repository.ContactMessageRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ContactMessageService {

    private static final String NEW_MESSAGE_TOPIC =
            "/topic/admin/contact-messages";

    private static final String UPDATED_MESSAGE_TOPIC =
            "/topic/admin/contact-message-updates";

    private static final String DELETED_MESSAGE_TOPIC =
            "/topic/admin/contact-message-deleted";

    private static final String MESSAGE_COUNT_TOPIC =
            "/topic/admin/contact-message-count";

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

        message.setFullName(
                request.getFullName().trim()
        );

        message.setEmail(
                request.getEmail()
                        .trim()
                        .toLowerCase()
        );

        if (
                request.getPhoneNumber() != null
                        && !request.getPhoneNumber().isBlank()
        ) {
            message.setPhoneNumber(
                    request.getPhoneNumber().trim()
            );
        } else {
            message.setPhoneNumber(null);
        }

        message.setSubject(
                request.getSubject().trim()
        );

        message.setMessage(
                request.getMessage().trim()
        );

        message.setStatus(
                ContactMessageStatus.NEW
        );

        ContactMessage saved =
                repository.save(message);

        ContactMessageResponse response =
                ContactMessageResponse.fromEntity(saved);

        messagingTemplate.convertAndSend(
                NEW_MESSAGE_TOPIC,
                response
        );

        broadcastNewMessageCount();

        return response;
    }

    @Transactional(readOnly = true)
    public List<ContactMessageResponse> findAll(
            ContactMessageStatus status
    ) {
        List<ContactMessage> messages;

        if (status == null) {
            messages =
                    repository.findAllByOrderByCreatedAtDesc();
        } else {
            messages =
                    repository.findByStatusOrderByCreatedAtDesc(
                            status
                    );
        }

        return messages.stream()
                .map(ContactMessageResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public ContactMessageResponse findById(Long id) {
        return ContactMessageResponse.fromEntity(
                findMessage(id)
        );
    }

    @Transactional
    public ContactMessageResponse update(
            Long id,
            UpdateContactMessageRequest request
    ) {
        ContactMessage message =
                findMessage(id);

        if (request.getStatus() != null) {
            message.setStatus(
                    request.getStatus()
            );
        }

        ContactMessage saved =
                repository.save(message);

        ContactMessageResponse response =
                ContactMessageResponse.fromEntity(saved);

        messagingTemplate.convertAndSend(
                UPDATED_MESSAGE_TOPIC,
                response
        );

        broadcastNewMessageCount();

        return response;
    }

    @Transactional
    public void delete(Long id) {
        ContactMessage message =
                findMessage(id);

        repository.delete(message);

        messagingTemplate.convertAndSend(
                DELETED_MESSAGE_TOPIC,
                id
        );

        broadcastNewMessageCount();
    }

    @Transactional(readOnly = true)
    public long getNewMessageCount() {
        return repository.countByStatus(
                ContactMessageStatus.NEW
        );
    }

    private void broadcastNewMessageCount() {
        long count =
                repository.countByStatus(
                        ContactMessageStatus.NEW
                );

        messagingTemplate.convertAndSend(
                MESSAGE_COUNT_TOPIC,
                count
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