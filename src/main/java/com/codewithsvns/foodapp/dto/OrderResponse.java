
package com.codewithsvns.foodapp.dto;

import com.codewithsvns.foodapp.model.OrderStatus;
import com.codewithsvns.foodapp.model.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID customerId,
        UUID restaurantId,
        UUID deliveryPartnerId,
        String restaurantName,
        OrderStatus status,
        PaymentStatus paymentStatus,
        BigDecimal totalAmount,
        String deliveryAddress,
        String customerNote,
        List<OrderLineResponse> items,
        Instant createdAt,
        Instant updatedAt,
        Instant preparationStartedAt,
        Integer estimatedPreparationMinutes,
        Instant estimatedPreparationCompletionAt,
        String restaurantAddress,
        String googleMapsDirectionsUrl
) {
    public record OrderLineResponse(
            UUID foodItemId,
            String foodName,
            BigDecimal unitPrice,
            int quantity,
            BigDecimal subtotal
    ) {}
}
