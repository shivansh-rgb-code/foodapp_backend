
package com.codewithsvns.foodapp.dto;

import com.codewithsvns.foodapp.model.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AvailableDeliveryOrderResponse(
        UUID id,
        UUID restaurantId,
        String restaurantName,
        OrderStatus status,
        BigDecimal totalAmount,
        List<OrderResponse.OrderLineResponse> items,
        Instant createdAt
) {
}
