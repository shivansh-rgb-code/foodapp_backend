
package com.codewithsvns.foodapp.controller;

import com.codewithsvns.foodapp.dto.AssignDeliveryPartnerRequest;
import com.codewithsvns.foodapp.dto.OrderResponse;
import com.codewithsvns.foodapp.dto.UpdateOrderStatusRequest;
import com.codewithsvns.foodapp.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/restaurant/me/orders")
public class RestaurantOrderController {

    private final OrderService orderService;

    public RestaurantOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public List<OrderResponse> getOrders(
            Authentication authentication) {
        return orderService.getRestaurantOrders(getUserId(authentication));
    }

    @PatchMapping("/{orderId}/status")
    public OrderResponse updateStatus(
            Authentication authentication,
            @PathVariable UUID orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request) {

        return orderService.updateRestaurantOrderStatus(
                getUserId(authentication),
                orderId,
                request.status());
    }

    @PostMapping("/{orderId}/assign-delivery")
    @ResponseStatus(HttpStatus.OK)
    public OrderResponse assignDeliveryPartner(
            Authentication authentication,
            @PathVariable UUID orderId,
            @Valid @RequestBody AssignDeliveryPartnerRequest request) {

        return orderService.assignDeliveryPartner(
                getUserId(authentication),
                orderId,
                request.deliveryPartnerUserId());
    }

    private UUID getUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication is required");
        }

        try {
            return UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid authenticated user");
        }
    }
}
