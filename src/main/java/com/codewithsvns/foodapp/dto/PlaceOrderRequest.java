package com.codewithsvns.foodapp.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record PlaceOrderRequest(

        @NotNull(message = "Restaurant ID is required")
        UUID restaurantId,

        @NotBlank(message = "Delivery address is required")
        @Size(max = 500, message = "Delivery address cannot exceed 500 characters")
        String deliveryAddress,

        @Size(max = 500, message = "Customer note cannot exceed 500 characters")
        String customerNote,

        @NotEmpty(message = "An order must contain at least one item")
        @Size(max = 50, message = "An order cannot contain more than 50 distinct items")
        List<@Valid OrderLineRequest> items
) {
}
