package com.codewithsvns.foodapp.controller;

import com.codewithsvns.foodapp.dto.OrderResponse;
import com.codewithsvns.foodapp.dto.PlaceOrderRequest;
import com.codewithsvns.foodapp.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/customer/orders")
public class CustomerOrderController {

    private final OrderService orderService;

    public CustomerOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse placeOrder(
            Authentication authentication,
            @Valid @RequestBody PlaceOrderRequest request) {

        UUID customerId = getAuthenticatedCustomerId(authentication);

        return orderService.placeOrder(customerId, request);
    }

    @GetMapping
    public List<OrderResponse> getMyOrders(
            Authentication authentication) {

        UUID customerId = getAuthenticatedCustomerId(authentication);

        return orderService.getMyOrders(customerId);
    }

    @GetMapping("/{orderId}")
    public OrderResponse getMyOrder(
            Authentication authentication,
            @PathVariable UUID orderId) {

        UUID customerId = getAuthenticatedCustomerId(authentication);

        return orderService.getMyOrder(customerId, orderId);
    }

    private UUID getAuthenticatedCustomerId(
            Authentication authentication) {

        if (authentication == null
                || authentication.getName() == null) {
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