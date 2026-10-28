package com.codewithsvns.foodapp.service;

import java.util.UUID;

public record DeliveryOrderAssignedEvent(
        UUID deliveryPartnerUserId,
        UUID orderId) {
}