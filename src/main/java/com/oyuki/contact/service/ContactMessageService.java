package com.oyuki.contact.service;

import com.oyuki.contact.dto.ContactMessageResponse;
import com.oyuki.contact.dto.CreateContactMessageRequest;
import com.oyuki.contact.dto.UpdateContactMessageRequest;
import com.oyuki.contact.entity.ContactMessage;
import com.oyuki.contact.enums.ContactMessageStatus;
import com.oyuki.contact.repository.ContactMessageRepository;
import com.oyuki.common.exception.ResourceNotFoundException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ContactMessageService {

    private final ContactMessageRepository repository;
    private final JavaMailSender mailSender;
    private final String senderEmail;
    private final String supportEmail;

    public ContactMessageService(
            ContactMessageRepository repository,
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${app.mail.from:}") String senderEmail,
            @Value("${app.contact.support-email:${app.mail.from:}}") String supportEmail
    ) {
        this.repository = repository;
        this.mailSender = mailSenderProvider.getIfAvailable();
        this.senderEmail = clean(senderEmail);
        this.supportEmail = clean(supportEmail);
    }

    @Transactional
    public ContactMessageResponse create(CreateContactMessageRequest request) {
        ContactMessage message = ContactMessage.builder()
                .name(request.name().trim())
                .email(request.email().trim().toLowerCase())
                .subject(request.subject().trim())
                .message(request.message().trim())
                .status(ContactMessageStatus.NEW)
                .build();

        ContactMessage saved = repository.save(message);
        sendAdminNotification(saved);
        sendCustomerConfirmation(saved);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ContactMessageResponse> findAll(ContactMessageStatus status) {
        List<ContactMessage> messages = status == null
                ? repository.findAllByOrderByCreatedAtDesc()
                : repository.findAllByStatusOrderByCreatedAtDesc(status);

        return messages.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ContactMessageResponse findById(Long id) {
        return toResponse(getMessage(id));
    }

    @Transactional
    public ContactMessageResponse update(Long id, UpdateContactMessageRequest request) {
        ContactMessage message = getMessage(id);

        if (request.status() != null) {
            message.setStatus(request.status());
        }

        if (request.reply() != null && !request.reply().isBlank()) {
            message.setAdminReply(request.reply().trim());
            message.setRepliedAt(LocalDateTime.now());
            message.setStatus(ContactMessageStatus.REPLIED);
            sendReplyEmail(message);
        }

        return toResponse(repository.save(message));
    }

    @Transactional
    public void delete(Long id) {
        ContactMessage message = getMessage(id);
        repository.delete(message);
    }

    private ContactMessage getMessage(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contact message not found"));
    }

    private void sendAdminNotification(ContactMessage contact) {
        if (!emailReady() || supportEmail.isBlank()) {
            return;
        }

        SimpleMailMessage email = new SimpleMailMessage();
        email.setFrom(senderEmail);
        email.setTo(supportEmail);
        email.setReplyTo(contact.getEmail());
        email.setSubject("New Oyuki contact message: " + contact.getSubject());
        email.setText(
                "Name: " + contact.getName() + "\n"
                        + "Email: " + contact.getEmail() + "\n"
                        + "Subject: " + contact.getSubject() + "\n\n"
                        + contact.getMessage()
        );
        safeSend(email);
    }

    private void sendCustomerConfirmation(ContactMessage contact) {
        if (!emailReady()) {
            return;
        }

        SimpleMailMessage email = new SimpleMailMessage();
        email.setFrom(senderEmail);
        email.setTo(contact.getEmail());
        email.setSubject("We received your message — Oyuki");
        email.setText(
                "Hello " + contact.getName() + ",\n\n"
                        + "We received your message about \"" + contact.getSubject() + "\". "
                        + "Our team will respond as soon as possible.\n\n"
                        + "Oyuki Marketplace"
        );
        safeSend(email);
    }

    private void sendReplyEmail(ContactMessage contact) {
        if (!emailReady()) {
            return;
        }

        SimpleMailMessage email = new SimpleMailMessage();
        email.setFrom(senderEmail);
        email.setTo(contact.getEmail());
        email.setSubject("Re: " + contact.getSubject());
        email.setText(
                "Hello " + contact.getName() + ",\n\n"
                        + contact.getAdminReply() + "\n\n"
                        + "Oyuki Marketplace"
        );
        safeSend(email);
    }

    private boolean emailReady() {
        return mailSender != null && !senderEmail.isBlank();
    }

    private void safeSend(SimpleMailMessage message) {
        try {
            mailSender.send(message);
        } catch (Exception exception) {
            System.err.println("Contact email delivery failed: " + exception.getMessage());
        }
    }

    private ContactMessageResponse toResponse(ContactMessage message) {
        return new ContactMessageResponse(
                message.getId(),
                message.getName(),
                message.getEmail(),
                message.getSubject(),
                message.getMessage(),
                message.getStatus(),
                message.getAdminReply(),
                message.getRepliedAt(),
                message.getCreatedAt(),
                message.getUpdatedAt()
        );
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
