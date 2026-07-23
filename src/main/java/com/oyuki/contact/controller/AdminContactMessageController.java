package com.oyuki.contact.controller;

import com.oyuki.common.response.ApiResponse;
import com.oyuki.contact.dto.ContactMessageResponse;
import com.oyuki.contact.dto.UpdateContactMessageRequest;
import com.oyuki.contact.enums.ContactMessageStatus;
import com.oyuki.contact.service.ContactMessageService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/contact-messages")
public class AdminContactMessageController {

    private final ContactMessageService service;

    public AdminContactMessageController(ContactMessageService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<ContactMessageResponse>> findAll(
            @RequestParam(required = false) ContactMessageStatus status
    ) {
        return ApiResponse.success("Contact messages retrieved", service.findAll(status));
    }

    @GetMapping("/{id}")
    public ApiResponse<ContactMessageResponse> findById(@PathVariable Long id) {
        return ApiResponse.success("Contact message retrieved", service.findById(id));
    }

    @PatchMapping("/{id}")
    public ApiResponse<ContactMessageResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateContactMessageRequest request
    ) {
        return ApiResponse.success("Contact message updated", service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Contact message deleted", null));
    }
}
