package com.oyuki.farmersday.controller;

import com.oyuki.farmersday.dto.CreateFarmersDayRequest;
import com.oyuki.farmersday.dto.FarmersDayResponse;
import com.oyuki.farmersday.enums.FarmersDayStatus;
import com.oyuki.farmersday.service.FarmersDayReminderService;
import com.oyuki.farmersday.service.FarmersDayService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
@RestController
@RequestMapping("/api/farmers-day")
public class FarmersDayController {

    private final FarmersDayService farmersDayService;
    private final FarmersDayReminderService farmersDayReminderService;

   public FarmersDayController(
        FarmersDayService farmersDayService,
        FarmersDayReminderService farmersDayReminderService
) {
    this.farmersDayService =
            farmersDayService;

    this.farmersDayReminderService =
            farmersDayReminderService;
}

    /*
     * =========================================================
     * ADMIN CREATES AN EVENT
     * =========================================================
     *
     * POST /api/farmers-day/admin
     */
    @PostMapping("/admin")
    public ResponseEntity<FarmersDayResponse>
    createEvent(
            Authentication authentication,
            @Valid @RequestBody
            CreateFarmersDayRequest request
    ) {
        Long adminId =
                getUserId(authentication);

        FarmersDayResponse response =
                farmersDayService.createEvent(
                        adminId,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /*
     * =========================================================
     * ADMIN EDITS AN EVENT
     * =========================================================
     *
     * PUT /api/farmers-day/admin/1
     */
    @PutMapping("/admin/{eventId}")
    public ResponseEntity<FarmersDayResponse>
    updateEvent(
            Authentication authentication,
            @PathVariable Long eventId,
            @Valid @RequestBody
            CreateFarmersDayRequest request
    ) {
        Long adminId =
                getUserId(authentication);

        return ResponseEntity.ok(
                farmersDayService.updateEvent(
                        adminId,
                        eventId,
                        request
                )
        );
    }

    /*
     * =========================================================
     * ADMIN VIEWS ALL EVENTS
     * =========================================================
     *
     * GET /api/farmers-day/admin
     *
     * GET /api/farmers-day/admin?status=DRAFT
     */
    @GetMapping("/admin")
    public ResponseEntity<List<FarmersDayResponse>>
    getAllEvents(
            Authentication authentication,

            @RequestParam(
                    required = false
            )
            FarmersDayStatus status
    ) {
        Long adminId =
                getUserId(authentication);

        return ResponseEntity.ok(
                farmersDayService.getAllEvents(
                        adminId,
                        status
                )
        );
    }

    /*
     * =========================================================
     * ADMIN VIEWS ONE EVENT
     * =========================================================
     */
    @GetMapping("/admin/{eventId}")
    public ResponseEntity<FarmersDayResponse>
    getAdminEvent(
            Authentication authentication,
            @PathVariable Long eventId
    ) {
        Long adminId =
                getUserId(authentication);

        return ResponseEntity.ok(
                farmersDayService.getAdminEvent(
                        adminId,
                        eventId
                )
        );
    }

    /*
     * =========================================================
     * ADMIN PUBLISHES EVENT
     * =========================================================
     *
     * Sends FARMERS_DAY_ANNOUNCEMENT notifications
     * to all active customers.
     */
    @PatchMapping("/admin/{eventId}/publish")
    public ResponseEntity<FarmersDayResponse>
    publishEvent(
            Authentication authentication,
            @PathVariable Long eventId
    ) {
        Long adminId =
                getUserId(authentication);

        return ResponseEntity.ok(
                farmersDayService.publishEvent(
                        adminId,
                        eventId
                )
        );
    }

    /*
     * =========================================================
     * ADMIN STARTS EVENT
     * =========================================================
     *
     * Sends FARMERS_DAY_STARTED notifications.
     */
    @PatchMapping("/admin/{eventId}/start")
    public ResponseEntity<FarmersDayResponse>
    startEvent(
            Authentication authentication,
            @PathVariable Long eventId
    ) {
        Long adminId =
                getUserId(authentication);

        return ResponseEntity.ok(
                farmersDayService.startEvent(
                        adminId,
                        eventId
                )
        );
    }

    /*
     * =========================================================
     * ADMIN COMPLETES EVENT
     * =========================================================
     */
    @PatchMapping("/admin/{eventId}/complete")
    public ResponseEntity<FarmersDayResponse>
    completeEvent(
            Authentication authentication,
            @PathVariable Long eventId
    ) {
        Long adminId =
                getUserId(authentication);

        return ResponseEntity.ok(
                farmersDayService.completeEvent(
                        adminId,
                        eventId
                )
        );
    }

    /*
     * =========================================================
     * ADMIN CANCELS EVENT
     * =========================================================
     */
    @PatchMapping("/admin/{eventId}/cancel")
    public ResponseEntity<FarmersDayResponse>
    cancelEvent(
            Authentication authentication,
            @PathVariable Long eventId
    ) {
        Long adminId =
                getUserId(authentication);

        return ResponseEntity.ok(
                farmersDayService.cancelEvent(
                        adminId,
                        eventId
                )
        );
    }

    /*
     * =========================================================
     * PUBLIC UPCOMING EVENTS
     * =========================================================
     *
     * GET /api/farmers-day/public/upcoming
     */
    @GetMapping("/public/upcoming")
    public ResponseEntity<List<FarmersDayResponse>>
    getUpcomingEvents() {
        return ResponseEntity.ok(
                farmersDayService
                        .getUpcomingPublicEvents()
        );
    }

    /*
     * =========================================================
     * PUBLIC VIEW OF ONE EVENT
     * =========================================================
     */
    @GetMapping("/public/{eventId}")
    public ResponseEntity<FarmersDayResponse>
    getPublicEvent(
            @PathVariable Long eventId
    ) {
        return ResponseEntity.ok(
                farmersDayService.getPublicEvent(
                        eventId
                )
        );
    }

    private Long getUserId(
            Authentication authentication
    ) {
        return (Long) authentication.getPrincipal();
    }

    @PostMapping("/admin/{eventId}/send-reminder")
public ResponseEntity<Map<String, Object>>
sendReminderManually(
        Authentication authentication,
        @PathVariable Long eventId
) {
    Long adminId =
            getUserId(authentication);

    /*
     * Confirm the logged-in user can access
     * this Farmers' Day event as an admin.
     */
    farmersDayService.getAdminEvent(
            adminId,
            eventId
    );

    int notificationCount =
            farmersDayReminderService
                    .sendReminderManually(
                            eventId
                    );

    return ResponseEntity.ok(
            Map.of(
                    "success",
                    true,
                    "message",
                    "Farmers' Day reminder sent",
                    "notificationCount",
                    notificationCount
            )
    );
}
}