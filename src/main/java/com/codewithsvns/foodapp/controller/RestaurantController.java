package com.codewithsvns.foodapp.controller;

import com.codewithsvns.foodapp.model.Restaurant;
import com.codewithsvns.foodapp.repository.RestaurantRepository;
import com.codewithsvns.foodapp.service.RegistrationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import org.springframework.web.bind.annotation.RequestBody;
import java.time.Instant;
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

    public record CuisineRequest(
            @NotBlank(message = "The cuisine is required")
            @Size(max = 60, message = "The cuisine must be at most 60 characters")
            String cuisine) {
    }

    public record LocationRequest(
            @NotNull(message = "Latitude is required")
            @DecimalMin(value = "-90.0")
            @DecimalMax(value = "90.0")
            Double latitude,

            @NotNull(message = "Longitude is required")
            @DecimalMin(value = "-180.0")
            @DecimalMax(value = "180.0")
            Double longitude) {
    }

    public record RestaurantResponse(
            UUID id,
            String name,
            String address,
            boolean open,
            String status,
            Instant createdAt,
            String cuisine,
            Double latitude,
            Double longitude,
            String imageUrl
    ) {}

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

    @PatchMapping("/me/cuisine")
    public RestaurantResponse updateCuisine(
            Authentication authentication,
            @Valid @RequestBody CuisineRequest request) {

        UUID ownerId = getAuthenticatedUserId(authentication);

        Restaurant restaurant = restaurantRepository.findByOwner_Id(ownerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No restaurant is associated with this account."));

        restaurant.setCuisine(request.cuisine().trim());

        Restaurant saved = restaurantRepository.save(restaurant);
        return toResponse(saved);
    }

    @PatchMapping("/me/location")
    public RestaurantResponse updateLocation(
            Authentication authentication,
            @Valid @RequestBody LocationRequest request) {

        UUID ownerId = getAuthenticatedUserId(authentication);

        Restaurant restaurant = restaurantRepository.findByOwner_Id(ownerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No restaurant is associated with this account."));

        restaurant.setLatitude(request.latitude());
        restaurant.setLongitude(request.longitude());

        return toResponse(restaurantRepository.save(restaurant));
    }


    @PatchMapping("/me/image")
    public RestaurantResponse updateImage(
            Authentication authentication,
            @Valid @RequestBody RestaurantImageRequest request
    ) {
        UUID userId = getAuthenticatedUserId(authentication);

        Restaurant restaurant = restaurantRepository.findByOwner_Id(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Restaurant not found"
                ));

        restaurant.setImageUrl(
                request.imageUrl() == null || request.imageUrl().isBlank()
                        ? null
                        : request.imageUrl().trim()
        );

        return toResponse(restaurantRepository.save(restaurant));
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
                restaurant.getStatus().toString(),
                restaurant.getCreatedAt(),
                restaurant.getCuisine(),
                restaurant.getLatitude(),
                restaurant.getLongitude(),
                restaurant.getImageUrl()
        );
    }

    public record RestaurantImageRequest(
            @jakarta.validation.constraints.Size(max = 1000)
            String imageUrl
    ) {}
}
