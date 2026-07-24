package com.oyuki.contact.dto;

import com.oyuki.contact.entity.ContactMessage;
import com.oyuki.contact.enums.ContactMessageStatus;

import java.time.LocalDateTime;

public class ContactMessageResponse {

    private Long id;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String subject;
    private String message;
    private ContactMessageStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ContactMessageResponse() {
    }

    public static ContactMessageResponse fromEntity(
            ContactMessage contactMessage
    ) {
        ContactMessageResponse response =
                new ContactMessageResponse();

        response.setId(contactMessage.getId());
        response.setFullName(contactMessage.getFullName());
        response.setEmail(contactMessage.getEmail());
        response.setPhoneNumber(contactMessage.getPhoneNumber());
        response.setSubject(contactMessage.getSubject());
        response.setMessage(contactMessage.getMessage());
        response.setStatus(contactMessage.getStatus());
        response.setCreatedAt(contactMessage.getCreatedAt());
        response.setUpdatedAt(contactMessage.getUpdatedAt());

        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public ContactMessageStatus getStatus() {
        return status;
    }

    public void setStatus(ContactMessageStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}