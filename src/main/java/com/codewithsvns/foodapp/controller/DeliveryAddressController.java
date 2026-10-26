package com.codewithsvns.foodapp.controller;

import com.codewithsvns.foodapp.dto.DeliveryAddressRequest;
import com.codewithsvns.foodapp.dto.DeliveryAddressResponse;
import com.codewithsvns.foodapp.model.UserRole;
import com.codewithsvns.foodapp.service.DeliveryAddressService;
import com.codewithsvns.foodapp.service.RegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/customer/addresses")
public class DeliveryAddressController {

    private final DeliveryAddressService deliveryAddressService;
    private final RegistrationService registrationService;

    public DeliveryAddressController(
            DeliveryAddressService deliveryAddressService,
            RegistrationService registrationService) {
        this.deliveryAddressService = deliveryAddressService;
        this.registrationService = registrationService;
    }

    @GetMapping
    public List<DeliveryAddressResponse> listAddresses(Authentication authentication) {
        UUID userId = extractAndAuthorize(authentication);
        return deliveryAddressService.list(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DeliveryAddressResponse createAddress(
            Authentication authentication,
            @Valid @RequestBody DeliveryAddressRequest request) {
        UUID userId = extractAndAuthorize(authentication);
        return deliveryAddressService.create(userId, request);
    }

    @GetMapping("/{id}")
    public DeliveryAddressResponse getAddress(
            Authentication authentication,
            @PathVariable UUID id) {
        UUID userId = extractAndAuthorize(authentication);
        return deliveryAddressService.getById(userId, id);
    }

    @PutMapping("/{id}")
    public DeliveryAddressResponse updateAddress(
            Authentication authentication,
            @PathVariable UUID id,
            @Valid @RequestBody DeliveryAddressRequest request) {
        UUID userId = extractAndAuthorize(authentication);
        return deliveryAddressService.update(userId, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAddress(
            Authentication authentication,
            @PathVariable UUID id) {
        UUID userId = extractAndAuthorize(authentication);
        deliveryAddressService.delete(userId, id);
    }

    @PatchMapping("/{id}/default")
    public DeliveryAddressResponse setDefaultAddress(
            Authentication authentication,
            @PathVariable UUID id) {
        UUID userId = extractAndAuthorize(authentication);
        return deliveryAddressService.setDefault(userId, id);
    }

    private UUID extractAndAuthorize(Authentication authentication) {
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
                    "Invalid authenticated user identifier.");
        }

        Map<String, Object> session =
                Map.of("user", Map.of("id", userId.toString()));

        registrationService.requireActiveProfile(session);
        registrationService.requireRole(session, UserRole.CUSTOMER);

        return userId;
    }
}
