package com.codewithsvns.foodapp.controller;

import com.codewithsvns.foodapp.ml.MlApiClient;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/customer/ml")
public class MlPreviousOrderRecommendationController {

    private final MlApiClient mlApiClient;

    public MlPreviousOrderRecommendationController(MlApiClient mlApiClient) {
        this.mlApiClient = mlApiClient;
    }

    @GetMapping("/previous-order-recommendations")
    public JsonNode recommendFromPreviousOrders(
            @RequestParam String customerId,
            @RequestParam(defaultValue = "10") Integer limit) {

        if (customerId == null || customerId.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Customer ID is required");
        }

        if (limit < 1 || limit > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Limit must be between 1 and 100");
        }

        return mlApiClient.recommendFromPreviousOrders(
                customerId.trim(), limit);
    }
}