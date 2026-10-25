
package com.codewithsvns.foodapp.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentOrderResponse(
        UUID orderId,
        String paymentMethod,
        String paymentStatus,
        BigDecimal amount,
        String currency,
        String razorpayOrderId,
        String razorpayKeyId
) {}
