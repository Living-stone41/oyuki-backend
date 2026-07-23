package com.oyuki.farmersday.service;

import com.oyuki.farmersday.dto.CreateFarmersDayRequest;
import com.oyuki.farmersday.dto.FarmersDayResponse;
import com.oyuki.farmersday.entity.FarmersDayEvent;
import com.oyuki.farmersday.enums.FarmersDayStatus;
import com.oyuki.farmersday.repository.FarmersDayRepository;
import com.oyuki.notification.enums.NotificationType;
import com.oyuki.notification.service.NotificationService;
import com.oyuki.user.entity.User;
import com.oyuki.user.enums.AccountStatus;
import com.oyuki.user.enums.Role;
import com.oyuki.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class FarmersDayService {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("d MMMM yyyy");

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("h:mm a");

    private final FarmersDayRepository farmersDayRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public FarmersDayService(
            FarmersDayRepository farmersDayRepository,
            UserRepository userRepository,
            NotificationService notificationService
    ) {
        this.farmersDayRepository =
                farmersDayRepository;

        this.userRepository =
                userRepository;

        this.notificationService =
                notificationService;
    }

    /*
     * =========================================================
     * ADMIN CREATES FARMERS' DAY
     * =========================================================
     */
    @Transactional
    public FarmersDayResponse createEvent(
            Long adminId,
            CreateFarmersDayRequest request
    ) {
        User admin = getActiveAdmin(adminId);

        validateRequest(request);

        FarmersDayEvent event =
                FarmersDayEvent.builder()
                        .title(
                                request.title().trim()
                        )
                        .description(
                                request.description().trim()
                        )
                        .eventDate(
                                request.eventDate()
                        )
                        .startTime(
                                request.startTime()
                        )
                        .endTime(
                                request.endTime()
                        )
                        .offerDetails(
                                clean(
                                        request.offerDetails()
                                )
                        )
                        .location(
                                clean(
                                        request.location()
                                )
                        )
                        .bannerImageUrl(
                                clean(
                                        request.bannerImageUrl()
                                )
                        )
                        .status(
                                FarmersDayStatus.DRAFT
                        )
                        .createdBy(admin)
                        .build();

        FarmersDayEvent savedEvent =
                farmersDayRepository.save(event);

        return FarmersDayResponse.from(
                savedEvent
        );
    }

    /*
     * =========================================================
     * ADMIN EDITS FARMERS' DAY
     * =========================================================
     *
     * Completed, active and cancelled events
     * cannot be edited.
     */
    @Transactional
    public FarmersDayResponse updateEvent(
            Long adminId,
            Long eventId,
            CreateFarmersDayRequest request
    ) {
        getActiveAdmin(adminId);

        validateRequest(request);

        FarmersDayEvent event =
                getEventOrThrow(eventId);

        if (
                event.getStatus()
                        == FarmersDayStatus.ACTIVE
                ||
                event.getStatus()
                        == FarmersDayStatus.COMPLETED
                ||
                event.getStatus()
                        == FarmersDayStatus.CANCELLED
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Active, completed or cancelled events cannot be edited"
            );
        }

        event.setTitle(
                request.title().trim()
        );

        event.setDescription(
                request.description().trim()
        );

        event.setEventDate(
                request.eventDate()
        );

        event.setStartTime(
                request.startTime()
        );

        event.setEndTime(
                request.endTime()
        );

        event.setOfferDetails(
                clean(
                        request.offerDetails()
                )
        );

        event.setLocation(
                clean(
                        request.location()
                )
        );

        event.setBannerImageUrl(
                clean(
                        request.bannerImageUrl()
                )
        );

        FarmersDayEvent savedEvent =
                farmersDayRepository.save(event);

        return FarmersDayResponse.from(
                savedEvent
        );
    }

    /*
     * =========================================================
     * ADMIN VIEWS ALL EVENTS
     * =========================================================
     */
    @Transactional(readOnly = true)
    public List<FarmersDayResponse> getAllEvents(
            Long adminId,
            FarmersDayStatus status
    ) {
        getActiveAdmin(adminId);

        List<FarmersDayEvent> events;

        if (status == null) {
            events =
                    farmersDayRepository
                            .findAllByOrderByEventDateDescStartTimeDesc();
        } else {
            events =
                    farmersDayRepository
                            .findAllByStatusOrderByEventDateAscStartTimeAsc(
                                    status
                            );
        }

        return events.stream()
                .map(FarmersDayResponse::from)
                .toList();
    }

    /*
     * =========================================================
     * ADMIN VIEWS ONE EVENT
     * =========================================================
     */
    @Transactional(readOnly = true)
    public FarmersDayResponse getAdminEvent(
            Long adminId,
            Long eventId
    ) {
        getActiveAdmin(adminId);

        FarmersDayEvent event =
                getEventOrThrow(eventId);

        return FarmersDayResponse.from(event);
    }

    /*
     * =========================================================
     * PUBLIC UPCOMING EVENTS
     * =========================================================
     *
     * Customers and website visitors can view
     * published or active Farmers' Day events.
     */
    @Transactional(readOnly = true)
    public List<FarmersDayResponse>
    getUpcomingPublicEvents() {

        List<FarmersDayStatus> visibleStatuses =
                List.of(
                        FarmersDayStatus.PUBLISHED,
                        FarmersDayStatus.ACTIVE
                );

        return farmersDayRepository
                .findAllByEventDateGreaterThanEqualAndStatusInOrderByEventDateAscStartTimeAsc(
                        LocalDate.now(),
                        visibleStatuses
                )
                .stream()
                .map(FarmersDayResponse::from)
                .toList();
    }

    /*
     * =========================================================
     * PUBLIC VIEW OF ONE EVENT
     * =========================================================
     */
    @Transactional(readOnly = true)
    public FarmersDayResponse getPublicEvent(
            Long eventId
    ) {
        FarmersDayEvent event =
                getEventOrThrow(eventId);

        if (
                event.getStatus()
                        == FarmersDayStatus.DRAFT
                ||
                event.getStatus()
                        == FarmersDayStatus.CANCELLED
        ) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Farmers' Day event not found"
            );
        }

        return FarmersDayResponse.from(event);
    }

    /*
     * =========================================================
     * ADMIN PUBLISHES EVENT
     * =========================================================
     *
     * Publishing sends an announcement to
     * every active customer.
     */
    @Transactional
    public FarmersDayResponse publishEvent(
            Long adminId,
            Long eventId
    ) {
        getActiveAdmin(adminId);

        FarmersDayEvent event =
                getEventOrThrow(eventId);

        if (
                event.getStatus()
                        != FarmersDayStatus.DRAFT
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only draft events can be published"
            );
        }

        event.setStatus(
                FarmersDayStatus.PUBLISHED
        );

        event.setPublishedAt(
                LocalDateTime.now()
        );

        FarmersDayEvent savedEvent =
                farmersDayRepository.save(event);

        notifyActiveCustomersAboutPublishedEvent(
                savedEvent
        );

        return FarmersDayResponse.from(
                savedEvent
        );
    }

    /*
     * =========================================================
     * ADMIN STARTS EVENT
     * =========================================================
     *
     * Starting the event sends another notification
     * telling customers that deals are now active.
     */
    @Transactional
    public FarmersDayResponse startEvent(
            Long adminId,
            Long eventId
    ) {
        getActiveAdmin(adminId);

        FarmersDayEvent event =
                getEventOrThrow(eventId);

        if (
                event.getStatus()
                        != FarmersDayStatus.PUBLISHED
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only published events can be started"
            );
        }

        event.setStatus(
                FarmersDayStatus.ACTIVE
        );

        event.setStartedAt(
                LocalDateTime.now()
        );

        FarmersDayEvent savedEvent =
                farmersDayRepository.save(event);

        notifyActiveCustomersEventStarted(
                savedEvent
        );

        return FarmersDayResponse.from(
                savedEvent
        );
    }

    /*
     * =========================================================
     * ADMIN COMPLETES EVENT
     * =========================================================
     */
    @Transactional
    public FarmersDayResponse completeEvent(
            Long adminId,
            Long eventId
    ) {
        getActiveAdmin(adminId);

        FarmersDayEvent event =
                getEventOrThrow(eventId);

        if (
                event.getStatus()
                        != FarmersDayStatus.ACTIVE
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only active events can be completed"
            );
        }

        event.setStatus(
                FarmersDayStatus.COMPLETED
        );

        event.setCompletedAt(
                LocalDateTime.now()
        );

        FarmersDayEvent savedEvent =
                farmersDayRepository.save(event);

        return FarmersDayResponse.from(
                savedEvent
        );
    }

    /*
     * =========================================================
     * ADMIN CANCELS EVENT
     * =========================================================
     */
    @Transactional
    public FarmersDayResponse cancelEvent(
            Long adminId,
            Long eventId
    ) {
        getActiveAdmin(adminId);

        FarmersDayEvent event =
                getEventOrThrow(eventId);

        if (
                event.getStatus()
                        == FarmersDayStatus.COMPLETED
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A completed event cannot be cancelled"
            );
        }

        if (
                event.getStatus()
                        == FarmersDayStatus.CANCELLED
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "This event has already been cancelled"
            );
        }

        event.setStatus(
                FarmersDayStatus.CANCELLED
        );

        FarmersDayEvent savedEvent =
                farmersDayRepository.save(event);

        return FarmersDayResponse.from(
                savedEvent
        );
    }

    /*
     * =========================================================
     * PUBLISH NOTIFICATIONS
     * =========================================================
     */
    private void notifyActiveCustomersAboutPublishedEvent(
            FarmersDayEvent event
    ) {
        List<User> customers =
                userRepository
                        .findAllByRoleAndStatus(
                                Role.CUSTOMER,
                                AccountStatus.ACTIVE
                        );

        String formattedDate =
                event.getEventDate()
                        .format(DATE_FORMATTER);

        String formattedStartTime =
                event.getStartTime()
                        .format(TIME_FORMATTER);

        String message =
                event.getTitle()
                        + " will hold on "
                        + formattedDate
                        + " at "
                        + formattedStartTime
                        + ". "
                        + buildOfferMessage(event);

        for (User customer : customers) {

            boolean alreadySent =
                    notificationService
                            .notificationAlreadyExists(
                                    customer.getId(),
                                    NotificationType
                                            .FARMERS_DAY_ANNOUNCEMENT,
                                    "FARMERS_DAY",
                                    event.getId()
                            );

            if (alreadySent) {
                continue;
            }

            notificationService.sendNotification(
                    customer,
                    NotificationType
                            .FARMERS_DAY_ANNOUNCEMENT,
                    "Farmers' Day is coming",
                    message,
                    "FARMERS_DAY",
                    event.getId(),
                    "/farmers-day/"
                            + event.getId(),
                    event.getBannerImageUrl()
            );
        }
    }

    /*
     * =========================================================
     * EVENT STARTED NOTIFICATIONS
     * =========================================================
     */
    private void notifyActiveCustomersEventStarted(
            FarmersDayEvent event
    ) {
        List<User> customers =
                userRepository
                        .findAllByRoleAndStatus(
                                Role.CUSTOMER,
                                AccountStatus.ACTIVE
                        );

        String message =
                event.getTitle()
                        + " has started. "
                        + buildOfferMessage(event)
                        + " Open the marketplace to view available deals.";

        for (User customer : customers) {

            boolean alreadySent =
                    notificationService
                            .notificationAlreadyExists(
                                    customer.getId(),
                                    NotificationType
                                            .FARMERS_DAY_STARTED,
                                    "FARMERS_DAY",
                                    event.getId()
                            );

            if (alreadySent) {
                continue;
            }

            notificationService.sendNotification(
                    customer,
                    NotificationType
                            .FARMERS_DAY_STARTED,
                    "Farmers' Day deals are live",
                    message,
                    "FARMERS_DAY",
                    event.getId(),
                    "/marketplace?farmersDay="
                            + event.getId(),
                    event.getBannerImageUrl()
            );
        }
    }

    /*
     * =========================================================
     * VALIDATION
     * =========================================================
     */
    private void validateRequest(
            CreateFarmersDayRequest request
    ) {
        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Farmers' Day request is required"
            );
        }

        if (
                request.title() == null ||
                request.title().isBlank()
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Event title is required"
            );
        }

        if (
                request.description() == null ||
                request.description().isBlank()
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Event description is required"
            );
        }

        if (request.eventDate() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Event date is required"
            );
        }

        if (
                request.eventDate()
                        .isBefore(LocalDate.now())
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Event date cannot be in the past"
            );
        }

        if (request.startTime() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Start time is required"
            );
        }

        if (request.endTime() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "End time is required"
            );
        }

        if (
                !request.endTime()
                        .isAfter(request.startTime())
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "End time must be after start time"
            );
        }
    }

    /*
     * =========================================================
     * GET ADMIN
     * =========================================================
     */
    private User getActiveAdmin(
            Long adminId
    ) {
        User admin =
                userRepository
                        .findById(adminId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Administrator account not found"
                                )
                        );

        if (admin.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only administrators can manage Farmers' Day events"
            );
        }

        if (
                admin.getStatus()
                        != AccountStatus.ACTIVE
        ) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "The administrator account is not active"
            );
        }

        return admin;
    }

    private FarmersDayEvent getEventOrThrow(
            Long eventId
    ) {
        return farmersDayRepository
                .findById(eventId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Farmers' Day event not found"
                        )
                );
    }

    private String buildOfferMessage(
            FarmersDayEvent event
    ) {
        if (
                event.getOfferDetails() == null ||
                event.getOfferDetails().isBlank()
        ) {
            return "Fresh farm products and special offers will be available.";
        }

        return event.getOfferDetails().trim();
    }

    private String clean(
            String value
    ) {
        if (
                value == null ||
                value.isBlank()
        ) {
            return null;
        }

        return value.trim();
    }
}