package com.codewithsvns.foodapp.dto;

import jakarta.validation.constraints.NotNull;

public record FoodItemAvailabilityRequest(
        @NotNull(message = "Availability is required")
        Boolean available
) {
}