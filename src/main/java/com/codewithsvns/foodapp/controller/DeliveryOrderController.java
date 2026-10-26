
package com.codewithsvns.foodapp.controller;

import com.codewithsvns.foodapp.dto.AvailableDeliveryOrderResponse;
import com.codewithsvns.foodapp.dto.OrderResponse;
import com.codewithsvns.foodapp.model.OrderStatus;
import com.codewithsvns.foodapp.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/delivery/me/orders")
public class DeliveryOrderController {

    private final OrderService orderService;

    public DeliveryOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    public record DeliveryStatusRequest(
            @NotNull(message = "Order status is required")
            OrderStatus status) {
    }

    @GetMapping
    public List<OrderResponse> getAssignedOrders(
            Authentication authentication) {

        return orderService.getAssignedOrders(
                getUserId(authentication));
    }

    @PatchMapping("/{orderId}/status")
    public OrderResponse updateDeliveryStatus(
            Authentication authentication,
            @PathVariable UUID orderId,
            @Valid @RequestBody DeliveryStatusRequest request) {

        return orderService.updateDeliveryStatus(
                getUserId(authentication),
                orderId,
                request.status());
    }

    @GetMapping("/available")
    public List<AvailableDeliveryOrderResponse> getAvailableOrders(
            Authentication authentication) {
        UUID userId = getUserId(authentication);
        return orderService.getAvailableDeliveryOrders(userId);
    }

    @PostMapping("/{orderId}/accept")
    public OrderResponse acceptOrder(
            @PathVariable UUID orderId,
            Authentication authentication
    ) {
        UUID userId = getUserId(authentication);
        return orderService.acceptDeliveryOrder(userId, orderId);
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
