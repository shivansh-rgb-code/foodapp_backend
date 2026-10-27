
package com.codewithsvns.foodapp.service;

import com.codewithsvns.foodapp.dto.AvailableDeliveryOrderResponse;
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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.time.Instant;

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
                .findByIdForUpdate(orderId)
                .filter(o -> o.getRestaurant().getId().equals(restaurant.getId()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Order not found"));


        if (newStatus == OrderStatus.CONFIRMED
                && order.getPaymentMethod() == PaymentMethod.RAZORPAY
                && order.getPaymentStatus() != PaymentStatus.PAID) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Razorpay payment must be completed before confirming the order");
        }

        if (newStatus == OrderStatus.CANCELLED
                && order.getPaymentMethod() == PaymentMethod.RAZORPAY
                && (order.getPaymentStatus() == PaymentStatus.PAID
                || order.getRazorpayOrderId() != null)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A Razorpay order cannot be cancelled after checkout starts or payment completes");
        }

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

        if (newStatus == OrderStatus.PREPARING) {
            order.setPreparationStartedAt(Instant.now());
        }

        order.setStatus(newStatus);

        return toResponse(customerOrderRepository.save(order));
    }


    @Transactional
    public OrderResponse assignDeliveryPartner(
            UUID restaurantOwnerId,
            UUID orderId,
            UUID deliveryPartnerUserId) {

        Map<String, Object> restaurantSession = sessionFor(restaurantOwnerId);
        registrationService.requireActiveProfile(restaurantSession);
        registrationService.requireRole(restaurantSession, UserRole.RESTAURANT);

        Restaurant restaurant = getRestaurant(restaurantOwnerId);

        Map<String, Object> partnerSession = sessionFor(deliveryPartnerUserId);
        registrationService.requireActiveProfile(partnerSession);
        registrationService.requireRole(partnerSession, UserRole.DELIVERY_PARTNER);

        DeliveryPartner partner = deliveryPartnerRepository
                .findByUserProfileIdForUpdate(deliveryPartnerUserId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Delivery partner profile not found"));

        if (!partner.isAvailable()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This delivery partner is not available");
        }

        if (customerOrderRepository.existsByDeliveryPartner_IdAndStatusIn(
                partner.getId(),
                List.of(OrderStatus.READY_FOR_PICKUP, OrderStatus.OUT_FOR_DELIVERY))) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This delivery partner already has an active delivery");
        }

        CustomerOrder order = customerOrderRepository
                .findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Order not found"));

        if (!order.getRestaurant().getId().equals(restaurant.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Order not found");
        }

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

        partner.setAvailable(false);
        order.setDeliveryPartner(partner);

        deliveryPartnerRepository.save(partner);
        return toResponse(customerOrderRepository.save(order));
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

        DeliveryPartner partner = deliveryPartnerRepository
                .findByUserProfileIdForUpdate(deliveryPartnerUserId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Delivery partner profile not found"));

        CustomerOrder order = customerOrderRepository
                .findByIdForUpdate(orderId)
                .filter(o -> o.getDeliveryPartner() != null
                        && o.getDeliveryPartner().getId().equals(partner.getId()))
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
            if (order.getPaymentMethod() == PaymentMethod.CASH_ON_DELIVERY) {
                order.setPaymentStatus(PaymentStatus.PAID);
            }
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


    @Transactional(readOnly = true)
    public List<AvailableDeliveryOrderResponse> getAvailableDeliveryOrders(
            UUID deliveryPartnerUserId) {

        Map<String, Object> session = sessionFor(deliveryPartnerUserId);
        registrationService.requireActiveProfile(session);
        registrationService.requireRole(session, UserRole.DELIVERY_PARTNER);

        DeliveryPartner partner = getDeliveryPartner(deliveryPartnerUserId);

        if (!partner.isAvailable()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "You must be available to view delivery jobs");
        }

        if (customerOrderRepository.existsByDeliveryPartner_IdAndStatusIn(
                partner.getId(),
                List.of(OrderStatus.READY_FOR_PICKUP, OrderStatus.OUT_FOR_DELIVERY))) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Complete your current delivery before accepting another job");
        }

        return customerOrderRepository
                .findByStatusAndDeliveryPartnerIsNullOrderByCreatedAtAsc(
                        OrderStatus.READY_FOR_PICKUP)
                .stream()
                .map(order -> new AvailableDeliveryOrderResponse(
                        order.getId(),
                        order.getRestaurant().getId(),
                        order.getRestaurant().getName(),
                        order.getStatus(),
                        order.getTotalAmount(),
                        order.getItems().stream()
                                .map(item -> new OrderResponse.OrderLineResponse(
                                        item.getFoodItem().getId(),
                                        item.getFoodName(),
                                        item.getUnitPrice(),
                                        item.getQuantity(),
                                        item.getSubtotal()))
                                .toList(),
                        order.getCreatedAt()))
                .toList();
    }

    @Transactional
    public OrderResponse acceptDeliveryOrder(
            UUID deliveryPartnerUserId,
            UUID orderId) {

        Map<String, Object> session = sessionFor(deliveryPartnerUserId);
        registrationService.requireActiveProfile(session);
        registrationService.requireRole(session, UserRole.DELIVERY_PARTNER);

        DeliveryPartner partner = deliveryPartnerRepository
                .findByUserProfileIdForUpdate(deliveryPartnerUserId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Delivery partner profile not found"));

        if (!partner.isAvailable()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "You are not available to accept a delivery");
        }

        if (customerOrderRepository.existsByDeliveryPartner_IdAndStatusIn(
                partner.getId(),
                List.of(OrderStatus.READY_FOR_PICKUP, OrderStatus.OUT_FOR_DELIVERY))) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "You already have an active delivery");
        }

        CustomerOrder order = customerOrderRepository
                .findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Delivery order not found"));

        if (order.getStatus() != OrderStatus.READY_FOR_PICKUP) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This order is not ready for pickup");
        }

        if (order.getDeliveryPartner() != null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This order has already been assigned");
        }

        partner.setAvailable(false);
        order.setDeliveryPartner(partner);

        deliveryPartnerRepository.save(partner);
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

        Instant preparationStartedAt = order.getPreparationStartedAt();

        Integer estimatedPreparationMinutes =
                preparationStartedAt == null ? null : 20;

        Instant estimatedPreparationCompletionAt =
                preparationStartedAt == null
                        ? null
                        : preparationStartedAt.plusSeconds(
                        estimatedPreparationMinutes * 60L);

        String restaurantAddress = order.getRestaurant().getAddress();

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
                order.getUpdatedAt(),
                preparationStartedAt,
                estimatedPreparationMinutes,
                estimatedPreparationCompletionAt,
                restaurantAddress,
                buildGoogleMapsDirectionsUrl(
                        restaurantAddress,
                        order.getDeliveryAddress())
        );
    }



    private String buildGoogleMapsDirectionsUrl(
            String restaurantAddress,
            String deliveryAddress) {

        if (restaurantAddress == null || restaurantAddress.isBlank()
                || deliveryAddress == null || deliveryAddress.isBlank()) {
            return null;
        }

        String origin = URLEncoder.encode(
                restaurantAddress, StandardCharsets.UTF_8);
        String destination = URLEncoder.encode(
                deliveryAddress, StandardCharsets.UTF_8);

        return "https://www.google.com/maps/dir/?api=1"
                + "&origin=" + origin
                + "&destination=" + destination
                + "&travelmode=driving";
    }
}
