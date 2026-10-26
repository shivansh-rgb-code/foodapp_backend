package com.codewithsvns.foodapp.dto;

import java.time.Instant;
import java.util.UUID;

public record DeliveryAddressResponse(
        UUID id,
        String label,
        String fullAddress,
        String street,
        String city,
        String state,
        String postalCode,
        Double latitude,
        Double longitude,
        String deliveryInstructions,
        boolean isDefault,
        Instant createdAt
) {
}
