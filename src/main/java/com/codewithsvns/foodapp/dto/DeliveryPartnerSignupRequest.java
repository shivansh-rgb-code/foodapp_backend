
package com.codewithsvns.foodapp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeliveryPartnerSignupRequest(

        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 60, message = "Name must be 2 to 60 characters")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Enter a valid email address")
        @Size(max = 254, message = "Email is too long")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 128, message = "Password must be 8 to 128 characters")
        String password

) {}
