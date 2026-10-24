package com.codewithsvns.foodapp.dto;

import com.codewithsvns.foodapp.model.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(
        @NotNull(message = "Order status is required")
        OrderStatus status
) {
}