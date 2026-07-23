package com.oyuki.newsletter.controller;

import com.oyuki.common.response.ApiResponse;
import com.oyuki.newsletter.dto.*;
import com.oyuki.newsletter.service.NewsletterService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/newsletter")
public class NewsletterController {
    private final NewsletterService service;
    public NewsletterController(NewsletterService service) { this.service = service; }

    @PostMapping("/subscribe")
    public ResponseEntity<ApiResponse<Map<String,Object>>> subscribe(@Valid @RequestBody NewsletterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Subscription successful. Check your inbox.", service.subscribe(request.email())));
    }

    @PostMapping("/unsubscribe")
    public ApiResponse<Map<String,Object>> unsubscribe(@Valid @RequestBody NewsletterRequest request) {
        return ApiResponse.success("You have been unsubscribed.", service.unsubscribe(request.email()));
    }

    @GetMapping("/admin/summary")
    public ApiResponse<Map<String,Object>> summary() {
        return ApiResponse.success("Newsletter summary", service.summary());
    }

    @PostMapping("/admin/send")
    public ApiResponse<Map<String,Object>> send(@Valid @RequestBody SendNewsletterRequest request) {
        return ApiResponse.success("Newsletter delivery completed", service.send(request.subject(), request.message()));
    }
}
