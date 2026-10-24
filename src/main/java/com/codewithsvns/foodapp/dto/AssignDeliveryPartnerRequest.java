
package com.codewithsvns.foodapp.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignDeliveryPartnerRequest(
        @NotNull(message = "Delivery partner user ID is required")
        UUID deliveryPartnerUserId
) {
}
