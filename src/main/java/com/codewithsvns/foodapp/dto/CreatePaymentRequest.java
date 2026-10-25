
package com.codewithsvns.foodapp.dto;

import com.codewithsvns.foodapp.model.PaymentMethod;
import jakarta.validation.constraints.NotNull;

public record CreatePaymentRequest(
        @NotNull(message = "Payment method is required")
        PaymentMethod paymentMethod
) {}
