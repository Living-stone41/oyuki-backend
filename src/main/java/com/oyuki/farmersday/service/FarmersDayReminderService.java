package com.oyuki.farmersday.service;

import com.oyuki.farmersday.entity.FarmersDayEvent;
import com.oyuki.farmersday.enums.FarmersDayStatus;
import com.oyuki.farmersday.repository.FarmersDayRepository;
import com.oyuki.notification.enums.NotificationType;
import com.oyuki.notification.service.NotificationService;
import com.oyuki.user.entity.User;
import com.oyuki.user.enums.AccountStatus;
import com.oyuki.user.enums.Role;
import com.oyuki.user.repository.UserRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class FarmersDayReminderService {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("d MMMM yyyy");

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("h:mm a");

    private final FarmersDayRepository farmersDayRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public FarmersDayReminderService(
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
     * Runs every day at 8:00 AM Nigeria time.
     *
     * It checks for published Farmers' Day events
     * happening the following day.
     */
    @Scheduled(
            cron = "0 0 8 * * *",
            zone = "Africa/Lagos"
    )
    @Transactional
    public void sendNextDayReminders() {

        LocalDate tomorrow =
                LocalDate.now()
                        .plusDays(1);

        List<FarmersDayEvent> events =
                farmersDayRepository
                        .findAllByEventDateAndStatus(
                                tomorrow,
                                FarmersDayStatus.PUBLISHED
                        );

        if (events.isEmpty()) {
            return;
        }

        List<User> activeCustomers =
                userRepository
                        .findAllByRoleAndStatus(
                                Role.CUSTOMER,
                                AccountStatus.ACTIVE
                        );

        for (FarmersDayEvent event : events) {
            sendEventReminder(
                    event,
                    activeCustomers
            );
        }
    }

    private void sendEventReminder(
            FarmersDayEvent event,
            List<User> customers
    ) {
        String formattedDate =
                event.getEventDate()
                        .format(DATE_FORMATTER);

        String formattedTime =
                event.getStartTime()
                        .format(TIME_FORMATTER);

        String offerMessage =
                event.getOfferDetails() == null ||
                event.getOfferDetails().isBlank()
                        ? "Fresh farm products and special offers will be available."
                        : event.getOfferDetails().trim();

        String message =
                event.getTitle()
                        + " is happening tomorrow, "
                        + formattedDate
                        + " at "
                        + formattedTime
                        + ". "
                        + offerMessage;

        for (User customer : customers) {

            boolean reminderAlreadySent =
                    notificationService
                            .notificationAlreadyExists(
                                    customer.getId(),
                                    NotificationType
                                            .FARMERS_DAY_REMINDER,
                                    "FARMERS_DAY",
                                    event.getId()
                            );

            if (reminderAlreadySent) {
                continue;
            }

            notificationService.sendNotification(
                    customer,
                    NotificationType
                            .FARMERS_DAY_REMINDER,
                    "Farmers' Day is tomorrow",
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
     * This method is useful for testing without
     * waiting until 8:00 AM.
     */
    @Transactional
    public int sendReminderManually(
            Long eventId
    ) {
        FarmersDayEvent event =
                farmersDayRepository
                        .findById(eventId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Farmers' Day event not found"
                                )
                        );

        List<User> customers =
                userRepository
                        .findAllByRoleAndStatus(
                                Role.CUSTOMER,
                                AccountStatus.ACTIVE
                        );

        int notificationCount = 0;

        for (User customer : customers) {

            boolean reminderAlreadySent =
                    notificationService
                            .notificationAlreadyExists(
                                    customer.getId(),
                                    NotificationType
                                            .FARMERS_DAY_REMINDER,
                                    "FARMERS_DAY",
                                    event.getId()
                            );

            if (reminderAlreadySent) {
                continue;
            }

            String offerMessage =
                    event.getOfferDetails() == null ||
                    event.getOfferDetails().isBlank()
                            ? "Fresh farm products and special offers will be available."
                            : event.getOfferDetails().trim();

            String message =
                    event.getTitle()
                            + " is coming soon. "
                            + offerMessage;

            notificationService.sendNotification(
                    customer,
                    NotificationType
                            .FARMERS_DAY_REMINDER,
                    "Farmers' Day reminder",
                    message,
                    "FARMERS_DAY",
                    event.getId(),
                    "/farmers-day/"
                            + event.getId(),
                    event.getBannerImageUrl()
            );

            notificationCount++;
        }

        return notificationCount;
    }
}