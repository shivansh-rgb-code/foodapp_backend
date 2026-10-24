package com.codewithsvns.foodapp.controller;

import com.codewithsvns.foodapp.dto.FoodItemAvailabilityRequest;
import com.codewithsvns.foodapp.dto.FoodItemRequest;
import com.codewithsvns.foodapp.dto.FoodItemResponse;
import com.codewithsvns.foodapp.model.FoodItem;
import com.codewithsvns.foodapp.model.Restaurant;
import com.codewithsvns.foodapp.model.UserRole;
import com.codewithsvns.foodapp.repository.FoodItemRepository;
import com.codewithsvns.foodapp.repository.RestaurantRepository;
import com.codewithsvns.foodapp.service.RegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/restaurant/me/food-items")
public class RestaurantFoodController {

    private final FoodItemRepository foodItemRepository;
    private final RestaurantRepository restaurantRepository;
    private final RegistrationService registrationService;

    public RestaurantFoodController(
            FoodItemRepository foodItemRepository,
            RestaurantRepository restaurantRepository,
            RegistrationService registrationService) {
        this.foodItemRepository = foodItemRepository;
        this.restaurantRepository = restaurantRepository;
        this.registrationService = registrationService;
    }

    @GetMapping
    public List<FoodItemResponse> getMyFoodItems(Authentication authentication) {
        Restaurant restaurant = getAuthenticatedRestaurant(authentication);

        return foodItemRepository.findByRestaurant_Id(restaurant.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FoodItemResponse createFoodItem(
            Authentication authentication,
            @Valid @RequestBody FoodItemRequest request) {

        Restaurant restaurant = getAuthenticatedRestaurant(authentication);

        FoodItem foodItem = new FoodItem(
                restaurant,
                request.name().trim(),
                request.description(),
                request.price(),
                request.imageUrl(),
                request.category()
        );

        return toResponse(foodItemRepository.save(foodItem));
    }

    @PutMapping("/{foodItemId}")
    public FoodItemResponse updateFoodItem(
            Authentication authentication,
            @PathVariable UUID foodItemId,
            @Valid @RequestBody FoodItemRequest request) {

        Restaurant restaurant = getAuthenticatedRestaurant(authentication);
        FoodItem foodItem = getOwnedFoodItem(restaurant, foodItemId);

        foodItem.setName(request.name().trim());
        foodItem.setDescription(request.description());
        foodItem.setPrice(request.price());
        foodItem.setImageUrl(request.imageUrl());
        foodItem.setCategory(request.category());

        return toResponse(foodItemRepository.save(foodItem));
    }

    @PatchMapping("/{foodItemId}/availability")
    public FoodItemResponse updateAvailability(
            Authentication authentication,
            @PathVariable UUID foodItemId,
            @Valid @RequestBody FoodItemAvailabilityRequest request) {

        Restaurant restaurant = getAuthenticatedRestaurant(authentication);
        FoodItem foodItem = getOwnedFoodItem(restaurant, foodItemId);

        foodItem.setAvailable(request.available());

        return toResponse(foodItemRepository.save(foodItem));
    }

    private Restaurant getAuthenticatedRestaurant(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Authentication is required");
        }

        UUID userId;

        try {
            userId = UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Invalid authenticated user");
        }

        registrationService.requireActiveProfile(
                java.util.Map.of("user", java.util.Map.of("id", userId.toString()))
        );

        registrationService.requireRole(
                java.util.Map.of("user", java.util.Map.of("id", userId.toString())),
                UserRole.RESTAURANT
        );

        return restaurantRepository.findByOwner_Id(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Restaurant profile not found"));
    }

    private FoodItem getOwnedFoodItem(Restaurant restaurant, UUID foodItemId) {
        FoodItem foodItem = foodItemRepository.findById(foodItemId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Food item not found"));

        if (!foodItem.getRestaurant().getId().equals(restaurant.getId())) {
            // Do not allow one restaurant to edit another restaurant's menu.
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Food item not found");
        }

        return foodItem;
    }

    private FoodItemResponse toResponse(FoodItem foodItem) {
        return new FoodItemResponse(
                foodItem.getId(),
                foodItem.getRestaurant().getId(),
                foodItem.getName(),
                foodItem.getDescription(),
                foodItem.getPrice(),
                foodItem.getImageUrl(),
                foodItem.isAvailable(),
                foodItem.getCategory(),
                foodItem.getCreatedAt()
        );
    }
}