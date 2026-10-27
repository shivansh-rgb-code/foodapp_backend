package com.codewithsvns.foodapp.ml;

import com.codewithsvns.foodapp.ml.dto.DeliveryPredictionRequest;
import tools.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;

@Component
public class MlApiClient {

    private final RestClient restClient;

    public MlApiClient(
            @Value("${ml.api.base-url}") String baseUrl
    ) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(60));

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }

    public JsonNode health() {
        return restClient.get()
                .uri("/health")
                .retrieve()
                .body(JsonNode.class);
    }

    public JsonNode predictDelivery(
            DeliveryPredictionRequest request
    ) {
        return restClient.post()
                .uri("/predict-delivery")
                .body(request)
                .retrieve()
                .body(JsonNode.class);
    }

    public JsonNode predictDelivery(Map<String, Object> features) {
        return restClient.post()
                .uri("/predict-delivery")
                .body(features)
                .retrieve()
                .body(JsonNode.class);
    }

    public JsonNode recommendRestaurants(
            String cuisine,
            String location,
            Double maxPrice,
            Double minRating,
            Integer limit
    ) {
        return restClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/restaurants/recommend");

                    if (cuisine != null && !cuisine.isBlank())
                        uriBuilder.queryParam("cuisine", cuisine);

                    if (location != null && !location.isBlank())
                        uriBuilder.queryParam("location", location);

                    if (maxPrice != null)
                        uriBuilder.queryParam("max_price", maxPrice);

                    if (minRating != null)
                        uriBuilder.queryParam("min_rating", minRating);

                    if (limit != null)
                        uriBuilder.queryParam("limit", limit);

                    return uriBuilder.build();
                })
                .retrieve()
                .body(JsonNode.class);
    }

    public JsonNode recommendFromPreviousOrders(
            String customerId,
            Integer limit
    ) {
        return restClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/restaurants/previous-orders");
                    uriBuilder.queryParam("customer_id", customerId);

                    if (limit != null)
                        uriBuilder.queryParam("limit", limit);

                    return uriBuilder.build();
                })
                .retrieve()
                .body(JsonNode.class);
    }
}
