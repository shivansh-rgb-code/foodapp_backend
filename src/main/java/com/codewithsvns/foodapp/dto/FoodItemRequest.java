package com.codewithsvns.foodapp.dto;

import com.codewithsvns.foodapp.model.FoodCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record FoodItemRequest(

        @NotBlank(message = "Food name is required")
        @Size(max = 150, message = "Food name cannot exceed 150 characters")
        String name,

        @Size(max = 1000, message = "Description cannot exceed 1000 characters")
        String description,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.01", message = "Price must be greater than zero")
        @Digits(integer = 8, fraction = 2, message = "Price can have at most 2 decimal places")
        BigDecimal price,

        @Size(max = 1000, message = "Image URL cannot exceed 1000 characters")
        String imageUrl,

        @NotNull(message = "Food category is required")
        FoodCategory category
) {
}