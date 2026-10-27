
package com.codewithsvns.foodapp.controller;

import com.codewithsvns.foodapp.ml.MlApiClient;
import tools.jackson.databind.JsonNode;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer/ml")
public class MlRecommendationController {

    private final MlApiClient mlApiClient;

    public MlRecommendationController(MlApiClient mlApiClient) {
        this.mlApiClient = mlApiClient;
    }

    @GetMapping("/health")
    public JsonNode health() {
        return mlApiClient.health();
    }

    @GetMapping("/restaurant-recommendations")
    public JsonNode recommendRestaurants(
            @RequestParam(required = false) String cuisine,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) Double minRating,
            @RequestParam(defaultValue = "10") Integer limit) {

        if (limit < 1 || limit > 100) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Limit must be between 1 and 100");
        }

        if (maxPrice != null && maxPrice < 0) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Maximum price cannot be negative");
        }

        if (minRating != null && (minRating < 0 || minRating > 5)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Minimum rating must be between 0 and 5");
        }

        return mlApiClient.recommendRestaurants(
                cuisine, location, maxPrice, minRating, limit);
    }
}
