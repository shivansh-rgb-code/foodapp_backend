package com.codewithsvns.foodapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

public record DeliveryAddressRequest(

        @NotBlank(message = "Label is required")
        @Size(max = 50, message = "Label cannot exceed 50 characters")
        String label,

        @NotBlank(message = "Full address is required")
        @Size(max = 500, message = "Full address cannot exceed 500 characters")
        String fullAddress,

        @Size(max = 255, message = "Street cannot exceed 255 characters")
        String street,

        @Size(max = 100, message = "City cannot exceed 100 characters")
        String city,

        @Size(max = 100, message = "State cannot exceed 100 characters")
        String state,

        @Size(max = 20, message = "Postal code cannot exceed 20 characters")
        String postalCode,

        @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
        @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
        Double latitude,

        @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
        @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
        Double longitude,

        @Size(max = 500, message = "Delivery instructions cannot exceed 500 characters")
        String deliveryInstructions,

        Boolean isDefault
) {
}
