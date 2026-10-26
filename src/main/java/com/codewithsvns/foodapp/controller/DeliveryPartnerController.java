
package com.codewithsvns.foodapp.controller;

import com.codewithsvns.foodapp.model.DeliveryPartner;
import com.codewithsvns.foodapp.model.UserRole;
import com.codewithsvns.foodapp.repository.DeliveryPartnerRepository;
import com.codewithsvns.foodapp.service.RegistrationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import com.codewithsvns.foodapp.model.OrderStatus;
import com.codewithsvns.foodapp.repository.CustomerOrderRepository;
import java.util.List;

@RestController
@RequestMapping("/api/delivery")
public class DeliveryPartnerController {

    private final DeliveryPartnerRepository deliveryPartnerRepository;
    private final RegistrationService registrationService;
    private final CustomerOrderRepository customerOrderRepository;

    public DeliveryPartnerController(
            DeliveryPartnerRepository deliveryPartnerRepository,
            CustomerOrderRepository customerOrderRepository,
            RegistrationService registrationService) {
        this.deliveryPartnerRepository = deliveryPartnerRepository;
        this.customerOrderRepository = customerOrderRepository;
        this.registrationService = registrationService;
    }

    public record AvailabilityRequest(
            @NotNull(message = "Availability is required")
            Boolean available) {
    }

    public record DeliveryPartnerResponse(
            UUID id,
            boolean available,
            Instant createdAt) {
    }

    @GetMapping("/me")
    public DeliveryPartnerResponse getMyProfile(
            Authentication authentication) {

        UUID userId = getAuthenticatedUserId(authentication);

        DeliveryPartner partner = deliveryPartnerRepository
                .findByUserProfile_Id(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No delivery-partner profile is associated with this account."));

        return toResponse(partner);
    }

    @PatchMapping("/me/availability")
    @Transactional
    public DeliveryPartnerResponse updateAvailability(
            Authentication authentication,
            @Valid @RequestBody AvailabilityRequest request) {

        UUID userId = getAuthenticatedUserId(authentication);

        DeliveryPartner partner = deliveryPartnerRepository
                .findByUserProfileIdForUpdate(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No delivery-partner profile is associated with this account."));

        if (Boolean.TRUE.equals(request.available())) {
            boolean hasActiveDelivery =
                    customerOrderRepository.existsByDeliveryPartner_IdAndStatusIn(
                            partner.getId(),
                            List.of(
                                    OrderStatus.READY_FOR_PICKUP,
                                    OrderStatus.OUT_FOR_DELIVERY
                            ));

            if (hasActiveDelivery) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "You cannot mark yourself available while you have an active delivery.");
            }
        }

        partner.setAvailable(request.available());

        DeliveryPartner saved = deliveryPartnerRepository.save(partner);

        return toResponse(saved);
    }

    private UUID getAuthenticatedUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication is required.");
        }

        UUID userId;
        try {
            userId = UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "The authenticated user identifier is invalid.");
        }

        Map<String, Object> session =
                Map.of("user", Map.of("id", userId.toString()));

        registrationService.requireActiveProfile(session);
        registrationService.requireRole(session, UserRole.DELIVERY_PARTNER);

        return userId;
    }

    private DeliveryPartnerResponse toResponse(DeliveryPartner partner) {
        return new DeliveryPartnerResponse(
                partner.getId(),
                partner.isAvailable(),
                partner.getCreatedAt());
    }
}
