package com.oyuki.delivery.service;

import com.oyuki.delivery.dto.CreateDeliveryRateRequest;
import com.oyuki.delivery.dto.DeliveryRateResponse;
import com.oyuki.delivery.dto.UpdateDeliveryRateRequest;
import com.oyuki.delivery.entity.DeliveryRate;
import com.oyuki.delivery.enums.DeliveryRateType;
import com.oyuki.delivery.enums.NigeriaState;
import com.oyuki.delivery.repository.DeliveryRateRepository;
import com.oyuki.user.entity.User;
import com.oyuki.user.enums.AccountStatus;
import com.oyuki.user.enums.Role;
import com.oyuki.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
public class DeliveryRateService {

    private final DeliveryRateRepository deliveryRateRepository;
    private final UserRepository userRepository;

    public DeliveryRateService(
            DeliveryRateRepository deliveryRateRepository,
            UserRepository userRepository
    ) {
        this.deliveryRateRepository = deliveryRateRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public DeliveryRateResponse createRate(
            Long adminId,
            CreateDeliveryRateRequest request
    ) {
        User admin = getActiveAdmin(adminId);

        validateRequest(
                request.rateType(),
                request.originState(),
                request.destinationState(),
                request.originCity(),
                request.destinationCity(),
                request.originArea(),
                request.destinationArea(),
                request.estimatedMinDays(),
                request.estimatedMaxDays()
        );

        DeliveryRate rate = new DeliveryRate();

        applyRateValues(
                rate,
                request.rateType(),
                request.originState(),
                request.destinationState(),
                request.originCity(),
                request.destinationCity(),
                request.originArea(),
                request.destinationArea(),
                request.fee(),
                request.estimatedMinDays(),
                request.estimatedMaxDays(),
                request.active()
        );

        rate.setCreatedBy(admin);
        rate.setUpdatedBy(admin);

        return DeliveryRateResponse.from(
                deliveryRateRepository.save(rate)
        );
    }

    @Transactional(readOnly = true)
    public List<DeliveryRateResponse> getRates(
            Long adminId,
            DeliveryRateType rateType,
            Boolean active
    ) {
        getActiveAdmin(adminId);

        return deliveryRateRepository
                .findAllByOrderByRateTypeAscOriginStateAscDestinationStateAscCreatedAtDesc()
                .stream()
                .filter(rate ->
                        rateType == null
                                || rate.getRateType() == rateType
                )
                .filter(rate ->
                        active == null
                                || rate.isActive() == active
                )
                .map(DeliveryRateResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public DeliveryRateResponse getRate(
            Long adminId,
            Long rateId
    ) {
        getActiveAdmin(adminId);

        return DeliveryRateResponse.from(
                getRateEntity(rateId)
        );
    }

    @Transactional
    public DeliveryRateResponse updateRate(
            Long adminId,
            Long rateId,
            UpdateDeliveryRateRequest request
    ) {
        User admin = getActiveAdmin(adminId);

        validateRequest(
                request.rateType(),
                request.originState(),
                request.destinationState(),
                request.originCity(),
                request.destinationCity(),
                request.originArea(),
                request.destinationArea(),
                request.estimatedMinDays(),
                request.estimatedMaxDays()
        );

        DeliveryRate rate = getRateEntity(rateId);

        applyRateValues(
                rate,
                request.rateType(),
                request.originState(),
                request.destinationState(),
                request.originCity(),
                request.destinationCity(),
                request.originArea(),
                request.destinationArea(),
                request.fee(),
                request.estimatedMinDays(),
                request.estimatedMaxDays(),
                request.active()
        );

        rate.setUpdatedBy(admin);

        return DeliveryRateResponse.from(
                deliveryRateRepository.save(rate)
        );
    }

    @Transactional
    public DeliveryRateResponse updateStatus(
            Long adminId,
            Long rateId,
            boolean active
    ) {
        User admin = getActiveAdmin(adminId);

        DeliveryRate rate = getRateEntity(rateId);

        rate.setActive(active);
        rate.setUpdatedBy(admin);

        return DeliveryRateResponse.from(
                deliveryRateRepository.save(rate)
        );
    }

    @Transactional
    public void deleteRate(
            Long adminId,
            Long rateId
    ) {
        getActiveAdmin(adminId);

        deliveryRateRepository.delete(
                getRateEntity(rateId)
        );
    }

    private void applyRateValues(
            DeliveryRate rate,
            DeliveryRateType rateType,
            NigeriaState originState,
            NigeriaState destinationState,
            String originCity,
            String destinationCity,
            String originArea,
            String destinationArea,
            BigDecimal fee,
            Integer estimatedMinDays,
            Integer estimatedMaxDays,
            Boolean active
    ) {
        rate.setRateType(rateType);
        rate.setFee(fee);

        int minDays =
                estimatedMinDays == null
                        ? 0
                        : estimatedMinDays;

        int maxDays =
                estimatedMaxDays == null
                        ? Math.max(1, minDays)
                        : estimatedMaxDays;

        rate.setEstimatedMinDays(minDays);
        rate.setEstimatedMaxDays(maxDays);
        rate.setActive(active == null || active);

        if (rateType == DeliveryRateType.ADDITIONAL_PROVIDER) {
            clearLocations(rate);
            return;
        }

        if (rateType == DeliveryRateType.INTERSTATE) {
            rate.setOriginState(originState);
            rate.setDestinationState(destinationState);
            rate.setOriginCity(clean(originCity));
            rate.setDestinationCity(clean(destinationCity));
            rate.setOriginArea(clean(originArea));
            rate.setDestinationArea(clean(destinationArea));
            return;
        }

        /*
         * Null location fields create nationwide fallback rates.
         * More specific state/city/area records override them.
         */
        rate.setOriginState(originState);
        rate.setDestinationState(originState);

        if (rateType == DeliveryRateType.SAME_STATE) {
            rate.setOriginCity(null);
            rate.setDestinationCity(null);
            rate.setOriginArea(null);
            rate.setDestinationArea(null);
            return;
        }

        String cleanedOriginCity = clean(originCity);
        String cleanedDestinationCity =
                clean(destinationCity) == null
                        ? cleanedOriginCity
                        : clean(destinationCity);

        rate.setOriginCity(cleanedOriginCity);
        rate.setDestinationCity(cleanedDestinationCity);

        if (rateType == DeliveryRateType.SAME_CITY) {
            rate.setOriginArea(null);
            rate.setDestinationArea(null);
            return;
        }

        String cleanedOriginArea = clean(originArea);
        String cleanedDestinationArea =
                clean(destinationArea) == null
                        ? cleanedOriginArea
                        : clean(destinationArea);

        rate.setOriginArea(cleanedOriginArea);
        rate.setDestinationArea(cleanedDestinationArea);
    }

    private void validateRequest(
            DeliveryRateType rateType,
            NigeriaState originState,
            NigeriaState destinationState,
            String originCity,
            String destinationCity,
            String originArea,
            String destinationArea,
            Integer estimatedMinDays,
            Integer estimatedMaxDays
    ) {
        if (rateType == null) {
            throw badRequest("Delivery rate type is required");
        }

        int minDays =
                estimatedMinDays == null
                        ? 0
                        : estimatedMinDays;

        int maxDays =
                estimatedMaxDays == null
                        ? Math.max(1, minDays)
                        : estimatedMaxDays;

        if (maxDays < minDays) {
            throw badRequest(
                    "Maximum delivery days cannot be less than minimum delivery days"
            );
        }

        if (rateType == DeliveryRateType.ADDITIONAL_PROVIDER) {
            return;
        }

        if (rateType == DeliveryRateType.INTERSTATE) {
            if (
                    originState != null
                            && destinationState != null
                            && originState == destinationState
            ) {
                throw badRequest(
                        "Interstate origin and destination states must be different"
                );
            }

            /*
             * Both states may be null for a nationwide interstate fallback.
             * One state may also be null to create a one-sided fallback.
             */
            return;
        }

        if (
                destinationState != null
                        && originState != null
                        && originState != destinationState
        ) {
            throw badRequest(
                    "Same-area, same-city and same-state rates must remain in one state"
            );
        }

        if (
                hasText(originCity)
                        && originState == null
        ) {
            throw badRequest(
                    "Origin state is required when a city is supplied"
            );
        }

        if (
                hasText(destinationCity)
                        && originState == null
        ) {
            throw badRequest(
                    "Origin state is required when a destination city is supplied"
            );
        }

        if (
                hasText(originArea)
                        && !hasText(originCity)
        ) {
            throw badRequest(
                    "Origin city is required when an area is supplied"
            );
        }

        if (
                hasText(destinationArea)
                        && !hasText(destinationCity)
                        && !hasText(originCity)
        ) {
            throw badRequest(
                    "A city is required when a destination area is supplied"
            );
        }
    }

    private void clearLocations(
            DeliveryRate rate
    ) {
        rate.setOriginState(null);
        rate.setDestinationState(null);
        rate.setOriginCity(null);
        rate.setDestinationCity(null);
        rate.setOriginArea(null);
        rate.setDestinationArea(null);
    }

    private DeliveryRate getRateEntity(
            Long rateId
    ) {
        return deliveryRateRepository
                .findById(rateId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Delivery rate not found"
                        )
                );
    }

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
                    "Only administrators can manage delivery rates"
            );
        }

        if (admin.getStatus() != AccountStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Administrator account is not active"
            );
        }

        return admin;
    }

    private ResponseStatusException badRequest(
            String message
    ) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                message
        );
    }

    private boolean hasText(
            String value
    ) {
        return clean(value) != null;
    }

    private String clean(
            String value
    ) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}
