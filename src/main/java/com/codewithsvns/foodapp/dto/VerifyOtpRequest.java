
package com.codewithsvns.foodapp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyOtpRequest(
        @NotBlank(message = "Email is required.")
        @Email(message = "Enter a valid email address.")
        String email,

        @NotBlank(message = "Verification code is required.")
        @Pattern(
                regexp = "\\d{6}",
                message = "Verification code must be exactly 6 digits."
        )
        String token
) {
}
