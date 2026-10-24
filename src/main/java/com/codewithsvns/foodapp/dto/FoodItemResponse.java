package com.codewithsvns.foodapp.dto;

import com.codewithsvns.foodapp.model.FoodCategory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FoodItemResponse(
        UUID id,
        UUID restaurantId,
        String name,
        String description,
        BigDecimal price,
        String imageUrl,
        boolean available,
        FoodCategory category,
        Instant createdAt
) {
}