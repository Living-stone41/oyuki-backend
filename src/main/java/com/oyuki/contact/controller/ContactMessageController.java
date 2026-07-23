package com.oyuki.contact.controller;

import com.oyuki.common.response.ApiResponse;
import com.oyuki.contact.dto.ContactMessageResponse;
import com.oyuki.contact.dto.CreateContactMessageRequest;
import com.oyuki.contact.service.ContactMessageService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/contact")
public class ContactMessageController {

    private final ContactMessageService service;

    public ContactMessageController(ContactMessageService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ContactMessageResponse>> create(
            @Valid @RequestBody CreateContactMessageRequest request
    ) {
        ContactMessageResponse response = service.create(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Message sent successfully. We will reply soon.",
                        response
                ));
    }
}
