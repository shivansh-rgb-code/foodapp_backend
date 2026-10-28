
package com.codewithsvns.foodapp.dto;

import com.codewithsvns.foodapp.model.OrderStatus;

import java.time.Instant;
import java.util.UUID;

public record OrderTrackingResponse(
        UUID orderId,
        OrderStatus status,
        boolean deliveryPartnerAssigned,
        Double deliveryPartnerLatitude,
        Double deliveryPartnerLongitude,
        Instant locationUpdatedAt
) {
}
