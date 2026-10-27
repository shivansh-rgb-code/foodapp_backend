package com.codewithsvns.foodapp.controller;

import com.codewithsvns.foodapp.ml.MlApiClient;
import com.codewithsvns.foodapp.model.OrderStatus;
import com.codewithsvns.foodapp.repository.CustomerOrderRepository;
import com.codewithsvns.foodapp.repository.CustomerOrderRepository.OrderEstimateInfo;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/customer/ml")
public class MlDeliveryPredictionController {

    private static final ZoneId INDIA = ZoneId.of("Asia/Kolkata");


    private static final List<String> MINUTE_KEYS = List.of(
            "predicted_delivery_time_minutes", "predicted_delivery_minutes",
            "predicted_minutes", "estimated_minutes", "delivery_time_minutes",
            "duration_minutes", "predicted_delivery_time", "delivery_time",
            "eta_minutes", "prediction");

    private final MlApiClient mlApiClient;
    private final CustomerOrderRepository customerOrderRepository;
    private final CustomerGuard customerGuard;

    public MlDeliveryPredictionController(
            MlApiClient mlApiClient,
            CustomerOrderRepository customerOrderRepository,
            CustomerGuard customerGuard) {
        this.mlApiClient = mlApiClient;
        this.customerOrderRepository = customerOrderRepository;
        this.customerGuard = customerGuard;
    }

    public record DeliveryEstimateResponse(
            Double estimatedMinutes,
            String distanceSource,
            JsonNode raw) {
    }

    @GetMapping("/orders/{orderId}/delivery-estimate")
    public DeliveryEstimateResponse estimateDelivery(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID orderId,
            @RequestParam(required = false) Double distanceKm,
            @RequestParam(required = false) String traffic,
            @RequestParam(required = false) String weather) {

        UUID userId = customerGuard.requireCustomer(jwt);

        if (distanceKm != null && (distanceKm <= 0 || distanceKm > 1000)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Distance must be between 0 and 1000 km");
        }

        OrderEstimateInfo info = customerOrderRepository
                .findEstimateInfo(orderId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Order not found"));

        if (info.getStatus() == OrderStatus.DELIVERED
                || info.getStatus() == OrderStatus.CANCELLED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "This order is already finished");
        }

        boolean haveCoordinates =
                info.getRestaurantLatitude() != null
                        && info.getRestaurantLongitude() != null
                        && info.getDeliveryLatitude() != null
                        && info.getDeliveryLongitude() != null;

        if (distanceKm == null && !haveCoordinates) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "This order has no saved location. Send distanceKm to get an estimate.");
        }

        ZonedDateTime orderedAt = info.getCreatedAt().atZone(INDIA);

        Map<String, Object> features = new HashMap<>();
        features.put("order_hour", orderedAt.getHour());
        features.put("order_minute", orderedAt.getMinute());
        features.put("order_weekday", orderedAt.getDayOfWeek().getValue() - 1); // 0 = Monday
        features.put("order_month", orderedAt.getMonthValue());

        if (haveCoordinates) {
            features.put("Restaurant_latitude", info.getRestaurantLatitude());
            features.put("Restaurant_longitude", info.getRestaurantLongitude());
            features.put("Delivery_location_latitude", info.getDeliveryLatitude());
            features.put("Delivery_location_longitude", info.getDeliveryLongitude());
        }

        if (distanceKm != null) {
            features.put("distance_km", distanceKm);
        }
        if (traffic != null && !traffic.isBlank()) {
            features.put("Road_traffic_density", traffic.trim());
        }
        if (weather != null && !weather.isBlank()) {
            features.put("Weatherconditions", weather.trim());
        }

        JsonNode raw = mlApiClient.predictDelivery(features);

        return new DeliveryEstimateResponse(
                extractMinutes(raw),
                distanceKm != null ? "request" : "coordinates",
                raw);
    }

    private Double extractMinutes(JsonNode json) {
        if (json == null) {
            return null;
        }
        if (json.isNumber()) {
            return json.doubleValue();
        }
        for (String key : MINUTE_KEYS) {
            JsonNode value = json.get(key);
            if (value != null && value.isNumber()) {
                return value.doubleValue();
            }
        }
        return null;
    }
}
