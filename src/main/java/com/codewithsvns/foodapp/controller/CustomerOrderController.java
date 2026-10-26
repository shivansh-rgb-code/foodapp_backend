
package com.codewithsvns.foodapp.controller;

import com.codewithsvns.foodapp.dto.OrderResponse;
import com.codewithsvns.foodapp.dto.PaymentOrderResponse;
import com.codewithsvns.foodapp.dto.PlaceOrderRequest;
import com.codewithsvns.foodapp.model.CustomerOrder;
import com.codewithsvns.foodapp.model.PaymentMethod;
import com.codewithsvns.foodapp.model.PaymentStatus;
import com.codewithsvns.foodapp.repository.CustomerOrderRepository;
import com.codewithsvns.foodapp.service.OrderService;
import com.codewithsvns.foodapp.service.RazorpayPaymentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import com.codewithsvns.foodapp.dto.VerifyPaymentRequest;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/customer/orders")
public class CustomerOrderController {

    private final OrderService orderService;
    private final RazorpayPaymentService razorpayPaymentService;
    private final CustomerOrderRepository customerOrderRepository;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    public CustomerOrderController(
            OrderService orderService,
            RazorpayPaymentService razorpayPaymentService,
            CustomerOrderRepository customerOrderRepository) {
        this.orderService = orderService;
        this.razorpayPaymentService = razorpayPaymentService;
        this.customerOrderRepository = customerOrderRepository;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse placeOrder(
            Authentication authentication,
            @Valid @RequestBody PlaceOrderRequest request,
            @RequestParam(defaultValue = "CASH_ON_DELIVERY")
            PaymentMethod paymentMethod) {

        UUID customerId = getAuthenticatedCustomerId(authentication);

        return orderService.placeOrder(
                customerId,
                request,
                paymentMethod
        );
    }

    @PostMapping("/with-payment-method")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse placeOrderWithPaymentMethod(
            Authentication authentication,
            @Valid @RequestBody PlaceOrderRequest request,
            @RequestParam PaymentMethod paymentMethod) {

        UUID customerId = getAuthenticatedCustomerId(authentication);

        return orderService.placeOrder(
                customerId,
                request,
                paymentMethod
        );
    }

    @PostMapping("/{orderId}/payment")
    public PaymentOrderResponse createPaymentOrder(
            Authentication authentication,
            @PathVariable UUID orderId) {

        UUID customerId = getAuthenticatedCustomerId(authentication);

        CustomerOrder order = customerOrderRepository
                .findByIdAndCustomer_Id(orderId, customerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Order not found"
                ));

        if (order.getPaymentMethod() != PaymentMethod.RAZORPAY) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "This order is not configured for Razorpay payment"
            );
        }

        String razorpayOrderId =
                razorpayPaymentService.createRazorpayOrder(order);

        CustomerOrder updatedOrder = customerOrderRepository
                .findById(order.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Order not found"
                ));

        return new PaymentOrderResponse(
                updatedOrder.getId(),
                updatedOrder.getPaymentMethod().name(),
                updatedOrder.getPaymentStatus().name(),
                updatedOrder.getTotalAmount(),
                "INR",
                razorpayOrderId,
                razorpayKeyId
        );
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

    @PostMapping("/{orderId}/payment/verify")
    public java.util.Map<String, String> verifyPayment(
            Authentication authentication,
            @PathVariable UUID orderId,
            @Valid @RequestBody VerifyPaymentRequest request) {

        UUID customerId = getAuthenticatedCustomerId(authentication);

        CustomerOrder order = customerOrderRepository
                .findByIdAndCustomer_Id(orderId, customerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Order not found"
                ));

        PaymentStatus paymentStatus = razorpayPaymentService.verifyPayment(
                orderId,
                customerId,
                request.razorpayOrderId(),
                request.razorpayPaymentId(),
                request.razorpaySignature());

        return Map.of(
                "message", "Payment signature verified",
                "orderId", orderId.toString(),
                "paymentStatus", paymentStatus.name());
    }

    private UUID getAuthenticatedCustomerId(
            Authentication authentication) {

        if (authentication == null
                || authentication.getName() == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication is required"
            );
        }

        try {
            return UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid authenticated user"
            );
        }
    }
}
