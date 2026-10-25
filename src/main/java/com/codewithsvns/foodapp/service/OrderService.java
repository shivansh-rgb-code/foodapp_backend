
package com.codewithsvns.foodapp.service;

import com.codewithsvns.foodapp.dto.OrderLineRequest;
import com.codewithsvns.foodapp.dto.OrderResponse;
import com.codewithsvns.foodapp.dto.PlaceOrderRequest;
import com.codewithsvns.foodapp.model.*;
import com.codewithsvns.foodapp.repository.CustomerOrderRepository;
import com.codewithsvns.foodapp.repository.DeliveryPartnerRepository;
import com.codewithsvns.foodapp.repository.FoodItemRepository;
import com.codewithsvns.foodapp.repository.RestaurantRepository;
import com.codewithsvns.foodapp.repository.UserProfileRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class OrderService {

    private final CustomerOrderRepository customerOrderRepository;
    private final FoodItemRepository foodItemRepository;
    private final RestaurantRepository restaurantRepository;
    private final UserProfileRepository userProfileRepository;
    private final DeliveryPartnerRepository deliveryPartnerRepository;
    private final RegistrationService registrationService;

    public OrderService(
            CustomerOrderRepository customerOrderRepository,
            FoodItemRepository foodItemRepository,
            RestaurantRepository restaurantRepository,
            UserProfileRepository userProfileRepository,
            DeliveryPartnerRepository deliveryPartnerRepository,
            RegistrationService registrationService) {

        this.customerOrderRepository = customerOrderRepository;
        this.foodItemRepository = foodItemRepository;
        this.restaurantRepository = restaurantRepository;
        this.userProfileRepository = userProfileRepository;
        this.deliveryPartnerRepository = deliveryPartnerRepository;
        this.registrationService = registrationService;
    }

    @Transactional
    public OrderResponse placeOrder(
            UUID customerId,
            PlaceOrderRequest request) {

        return placeOrder(
                customerId,
                request,
                PaymentMethod.CASH_ON_DELIVERY
        );
    }

    @Transactional
    public OrderResponse placeOrder(
            UUID customerId,
            PlaceOrderRequest request,
            PaymentMethod paymentMethod) {

        if (paymentMethod == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Payment method is required");
        }

        Map<String, Object> session = sessionFor(customerId);
        registrationService.requireActiveProfile(session);
        registrationService.requireRole(session, UserRole.CUSTOMER);

        UserProfile customer = userProfileRepository.findById(customerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Customer profile not found"));

        Restaurant restaurant = restaurantRepository
                .findById(request.restaurantId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Restaurant not found"));

        if (restaurant.getStatus() != RestaurantStatus.APPROVED
                || !restaurant.isOpen()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This restaurant is currently unavailable");
        }

        if (request.items() == null || request.items().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "An order must contain at least one item");
        }

        if (request.items().size() > 50) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "An order cannot contain more than 50 distinct items");
        }

        HashSet<UUID> seenFoodItemIds = new HashSet<>();
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (OrderLineRequest line : request.items()) {
            if (line == null || line.foodItemId() == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid order item");
            }

            if (line.quantity() < 1 || line.quantity() > 100) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Each item quantity must be between 1 and 100");
            }

            if (!seenFoodItemIds.add(line.foodItemId())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Each food item must appear only once; adjust its quantity instead");
            }

            FoodItem foodItem = foodItemRepository
                    .findById(line.foodItemId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "One or more food items are invalid"));

            if (!foodItem.getRestaurant().getId()
                    .equals(restaurant.getId())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "All items must belong to the selected restaurant");
            }

            if (!foodItem.isAvailable()) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "A selected food item is no longer available");
            }

            BigDecimal unitPrice = foodItem.getPrice();
            BigDecimal subtotal = unitPrice.multiply(
                    BigDecimal.valueOf(line.quantity()));

            orderItems.add(new OrderItem(
                    foodItem,
                    foodItem.getName(),
                    unitPrice,
                    line.quantity(),
                    subtotal
            ));

            total = total.add(subtotal);
        }

        CustomerOrder order = new CustomerOrder(
                customer,
                restaurant,
                total,
                request.deliveryAddress().trim(),
                request.customerNote() == null
                        ? null : request.customerNote().trim()
        );

        order.setPaymentMethod(paymentMethod);

        orderItems.forEach(order::addItem);

        return toResponse(customerOrderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(UUID customerId) {
        Map<String, Object> session = sessionFor(customerId);
        registrationService.requireActiveProfile(session);
        registrationService.requireRole(session, UserRole.CUSTOMER);

        return customerOrderRepository
                .findByCustomer_IdOrderByCreatedAtDesc(customerId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getMyOrder(UUID customerId, UUID orderId) {
        Map<String, Object> session = sessionFor(customerId);
        registrationService.requireActiveProfile(session);
        registrationService.requireRole(session, UserRole.CUSTOMER);

        CustomerOrder order = customerOrderRepository
                .findByIdAndCustomer_Id(orderId, customerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Order not found"));

        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getRestaurantOrders(UUID restaurantOwnerId) {
        Map<String, Object> session = sessionFor(restaurantOwnerId);
        registrationService.requireActiveProfile(session);
        registrationService.requireRole(session, UserRole.RESTAURANT);

        Restaurant restaurant = getRestaurant(restaurantOwnerId);

        return customerOrderRepository
                .findByRestaurant_IdOrderByCreatedAtDesc(restaurant.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public OrderResponse updateRestaurantOrderStatus(
            UUID restaurantOwnerId,
            UUID orderId,
            OrderStatus newStatus) {

        Map<String, Object> session = sessionFor(restaurantOwnerId);
        registrationService.requireActiveProfile(session);
        registrationService.requireRole(session, UserRole.RESTAURANT);

        Restaurant restaurant = getRestaurant(restaurantOwnerId);

        CustomerOrder order = customerOrderRepository
                .findByIdAndRestaurant_Id(orderId, restaurant.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Order not found"));

        OrderStatus current = order.getStatus();

        boolean allowed = switch (current) {
            case PENDING ->
                    newStatus == OrderStatus.CONFIRMED
                            || newStatus == OrderStatus.CANCELLED;
            case CONFIRMED ->
                    newStatus == OrderStatus.PREPARING
                            || newStatus == OrderStatus.CANCELLED;
            case PREPARING ->
                    newStatus == OrderStatus.READY_FOR_PICKUP;
            default -> false;
        };

        if (!allowed) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot change order status from "
                            + current + " to " + newStatus);
        }

        order.setStatus(newStatus);

        return toResponse(customerOrderRepository.save(order));
    }

    @Transactional
    public OrderResponse assignDeliveryPartner(
            UUID restaurantOwnerId,
            UUID orderId,
            UUID deliveryPartnerUserId) {

        Map<String, Object> restaurantSession =
                sessionFor(restaurantOwnerId);

        registrationService.requireActiveProfile(restaurantSession);
        registrationService.requireRole(
                restaurantSession,
                UserRole.RESTAURANT);

        Restaurant restaurant = getRestaurant(restaurantOwnerId);

        CustomerOrder order = customerOrderRepository
                .findByIdAndRestaurant_Id(orderId, restaurant.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Order not found"));

        if (order.getStatus() != OrderStatus.READY_FOR_PICKUP) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A delivery partner can only be assigned when the order is ready for pickup");
        }

        if (order.getDeliveryPartner() != null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A delivery partner is already assigned to this order");
        }

        Map<String, Object> partnerSession =
                sessionFor(deliveryPartnerUserId);

        registrationService.requireActiveProfile(partnerSession);
        registrationService.requireRole(
                partnerSession,
                UserRole.DELIVERY_PARTNER);

        DeliveryPartner partner = deliveryPartnerRepository
                .findByUserProfile_Id(deliveryPartnerUserId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Delivery partner profile not found"));

        if (!partner.isAvailable()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This delivery partner is not available");
        }

        partner.setAvailable(false);
        order.setDeliveryPartner(partner);

        deliveryPartnerRepository.save(partner);
        CustomerOrder savedOrder = customerOrderRepository.save(order);

        return toResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getAssignedOrders(
            UUID deliveryPartnerUserId) {

        Map<String, Object> session = sessionFor(deliveryPartnerUserId);
        registrationService.requireActiveProfile(session);
        registrationService.requireRole(
                session,
                UserRole.DELIVERY_PARTNER);

        DeliveryPartner partner = getDeliveryPartner(deliveryPartnerUserId);

        return customerOrderRepository
                .findByDeliveryPartner_IdOrderByCreatedAtDesc(
                        partner.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public OrderResponse updateDeliveryStatus(
            UUID deliveryPartnerUserId,
            UUID orderId,
            OrderStatus newStatus) {

        Map<String, Object> session = sessionFor(deliveryPartnerUserId);
        registrationService.requireActiveProfile(session);
        registrationService.requireRole(
                session,
                UserRole.DELIVERY_PARTNER);

        DeliveryPartner partner = getDeliveryPartner(deliveryPartnerUserId);

        CustomerOrder order = customerOrderRepository
                .findByIdAndDeliveryPartner_Id(orderId, partner.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Assigned order not found"));

        OrderStatus current = order.getStatus();

        if (current == OrderStatus.READY_FOR_PICKUP
                && newStatus == OrderStatus.OUT_FOR_DELIVERY) {
            order.setStatus(OrderStatus.OUT_FOR_DELIVERY);
        } else if (current == OrderStatus.OUT_FOR_DELIVERY
                && newStatus == OrderStatus.DELIVERED) {
            order.setStatus(OrderStatus.DELIVERED);
            partner.setAvailable(true);
            deliveryPartnerRepository.save(partner);
        } else {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot change delivery status from "
                            + current + " to " + newStatus);
        }

        return toResponse(customerOrderRepository.save(order));
    }

    private Restaurant getRestaurant(UUID ownerId) {
        return restaurantRepository.findByOwner_Id(ownerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Restaurant profile not found"));
    }

    private DeliveryPartner getDeliveryPartner(UUID userId) {
        return deliveryPartnerRepository.findByUserProfile_Id(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Delivery partner profile not found"));
    }

    private Map<String, Object> sessionFor(UUID userId) {
        return Map.of(
                "user",
                Map.of("id", userId.toString())
        );
    }

    private OrderResponse toResponse(CustomerOrder order) {
        List<OrderResponse.OrderLineResponse> lines =
                order.getItems().stream()
                        .map(item -> new OrderResponse.OrderLineResponse(
                                item.getFoodItem().getId(),
                                item.getFoodName(),
                                item.getUnitPrice(),
                                item.getQuantity(),
                                item.getSubtotal()))
                        .toList();

        UUID deliveryPartnerId = order.getDeliveryPartner() == null
                ? null
                : order.getDeliveryPartner().getId();

        return new OrderResponse(
                order.getId(),
                order.getCustomer().getId(),
                order.getRestaurant().getId(),
                deliveryPartnerId,
                order.getRestaurant().getName(),
                order.getStatus(),
                order.getPaymentStatus(),
                order.getTotalAmount(),
                order.getDeliveryAddress(),
                order.getCustomerNote(),
                lines,
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
