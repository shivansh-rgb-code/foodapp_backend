
package com.codewithsvns.foodapp.controller;

import com.codewithsvns.foodapp.model.Restaurant;
import com.codewithsvns.foodapp.repository.RestaurantRepository;
import com.codewithsvns.foodapp.service.RegistrationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/restaurant")
public class RestaurantController {

    private final RestaurantRepository restaurantRepository;
    private final RegistrationService registrationService;

    public RestaurantController(
            RestaurantRepository restaurantRepository,
            RegistrationService registrationService) {
        this.restaurantRepository = restaurantRepository;
        this.registrationService = registrationService;
    }

    public record OpenStatusRequest(
            @NotNull(message = "The open status is required")
            Boolean open) {
    }

    public record RestaurantResponse(
            UUID id,
            String name,
            String address,
            boolean open,
            String status,
            java.time.Instant createdAt) {
    }

    @GetMapping("/me")
    public RestaurantResponse getMyRestaurant(Authentication authentication) {
        UUID ownerId = getAuthenticatedUserId(authentication);

        Restaurant restaurant = restaurantRepository.findByOwner_Id(ownerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No restaurant is associated with this account."));

        return toResponse(restaurant);
    }

    @PatchMapping("/me/open-status")
    public RestaurantResponse updateOpenStatus(
            Authentication authentication,
            @Valid @RequestBody OpenStatusRequest request) {

        UUID ownerId = getAuthenticatedUserId(authentication);

        Restaurant restaurant = restaurantRepository.findByOwner_Id(ownerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No restaurant is associated with this account."));

        if (restaurant.getStatus()
                != com.codewithsvns.foodapp.model.RestaurantStatus.APPROVED) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Your restaurant must be approved before you can open it.");
        }

        restaurant.setOpen(request.open());

        Restaurant saved = restaurantRepository.save(restaurant);
        return toResponse(saved);
    }

    private UUID getAuthenticatedUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Authentication is required.");
        }

        UUID userId;
        try {
            userId = UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "The authenticated user identifier is invalid.");
        }

        registrationService.requireActiveProfile(
                Map.of("user", Map.of("id", userId.toString())));

        registrationService.requireRole(
                Map.of("user", Map.of("id", userId.toString())),
                com.codewithsvns.foodapp.model.UserRole.RESTAURANT);

        return userId;
    }

    private RestaurantResponse toResponse(Restaurant restaurant) {
        return new RestaurantResponse(
                restaurant.getId(),
                restaurant.getName(),
                restaurant.getAddress(),
                restaurant.isOpen(),
                restaurant.getStatus().name(),
                restaurant.getCreatedAt());
    }
}
