package com.codewithsvns.foodapp.controller;

import com.codewithsvns.foodapp.dto.FoodItemResponse;
import com.codewithsvns.foodapp.model.FoodItem;
import com.codewithsvns.foodapp.model.FoodCategory;
import com.codewithsvns.foodapp.model.Restaurant;
import com.codewithsvns.foodapp.model.RestaurantStatus;
import com.codewithsvns.foodapp.repository.FoodItemRepository;
import com.codewithsvns.foodapp.repository.RestaurantRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/customer/restaurants")
public class CustomerBrowseController {

    private final RestaurantRepository restaurantRepository;
    private final FoodItemRepository foodItemRepository;

    public CustomerBrowseController(
            RestaurantRepository restaurantRepository,
            FoodItemRepository foodItemRepository) {
        this.restaurantRepository = restaurantRepository;
        this.foodItemRepository = foodItemRepository;
    }

    @GetMapping
    public List<RestaurantResponse> getApprovedRestaurants(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean open) {
        return restaurantRepository.searchApproved(normalizeQuery(q), open)
                .stream()
                .map(this::toRestaurantResponse)
                .toList();
    }

    @GetMapping("/{restaurantId}")
    public RestaurantResponse getRestaurant(
            @PathVariable UUID restaurantId) {

        Restaurant restaurant = getApprovedRestaurant(restaurantId);
        return toRestaurantResponse(restaurant);
    }

    @GetMapping("/{restaurantId}/food-items")
    public List<FoodItemResponse> getAvailableFoodItems(
            @PathVariable UUID restaurantId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) FoodCategory category) {

        Restaurant restaurant = getApprovedRestaurant(restaurantId);

        return foodItemRepository
                .searchAvailable(restaurant.getId(), category, normalizeQuery(q))
                .stream()
                .map(this::toFoodItemResponse)
                .toList();
    }

    private String normalizeQuery(String query) {
        if (query == null || query.isBlank()) {
            return "";
        }
        return query.trim();
    }

    private Restaurant getApprovedRestaurant(UUID restaurantId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Restaurant not found"));

        if (restaurant.getStatus() != RestaurantStatus.APPROVED) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Restaurant not found");
        }

        return restaurant;
    }

    private RestaurantResponse toRestaurantResponse(Restaurant restaurant) {
        return new RestaurantResponse(
                restaurant.getId(),
                restaurant.getName(),
                restaurant.getAddress(),
                restaurant.isOpen(),
                restaurant.getStatus().name(),
                restaurant.getCreatedAt()
        );
    }

    private FoodItemResponse toFoodItemResponse(FoodItem foodItem) {
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

    public record RestaurantResponse(
            UUID id,
            String name,
            String address,
            boolean open,
            String status,
            java.time.Instant createdAt
    ) {
    }
}
