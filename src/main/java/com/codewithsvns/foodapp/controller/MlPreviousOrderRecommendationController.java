package com.codewithsvns.foodapp.controller;

import com.codewithsvns.foodapp.ml.MlApiClient;
import com.codewithsvns.foodapp.repository.CustomerOrderRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/customer/ml")
public class MlPreviousOrderRecommendationController {

    private static final int MAX_CUISINES = 3;

    private final MlApiClient mlApiClient;
    private final CustomerOrderRepository customerOrderRepository;
    private final CustomerGuard customerGuard;

    public MlPreviousOrderRecommendationController(
            MlApiClient mlApiClient,
            CustomerOrderRepository customerOrderRepository,
            CustomerGuard customerGuard) {
        this.mlApiClient = mlApiClient;
        this.customerOrderRepository = customerOrderRepository;
        this.customerGuard = customerGuard;
    }

    public record CuisineRecommendations(String cuisine, JsonNode results) {
    }

    public record ForYouResponse(
            boolean personalised,
            List<String> basedOnCuisines,
            List<CuisineRecommendations> recommendations) {
    }

    @GetMapping("/previous-order-recommendations")
    public ForYouResponse recommendFromPreviousOrders(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) Double minRating,
            @RequestParam(defaultValue = "10") Integer limit) {

        if (limit < 1 || limit > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Limit must be between 1 and 100");
        }
        if (maxPrice != null && maxPrice < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Maximum price cannot be negative");
        }
        if (minRating != null && (minRating < 0 || minRating > 5)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Minimum rating must be between 0 and 5");
        }

        UUID userId = customerGuard.requireCustomer(jwt);

        List<String> cuisines = customerOrderRepository
                .findTopCuisines(userId, PageRequest.of(0, MAX_CUISINES));

        List<CuisineRecommendations> recommendations = new ArrayList<>();

        if (cuisines.isEmpty()) {
            recommendations.add(new CuisineRecommendations(
                    null,
                    mlApiClient.recommendRestaurants(
                            null, location, maxPrice, minRating, limit)));
            return new ForYouResponse(false, List.of(), recommendations);
        }

        for (String cuisine : cuisines) {
            recommendations.add(new CuisineRecommendations(
                    cuisine,
                    mlApiClient.recommendRestaurants(
                            cuisine, location, maxPrice, minRating, limit)));
        }

        return new ForYouResponse(true, cuisines, recommendations);
    }
}
