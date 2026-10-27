package com.codewithsvns.foodapp.controller;

import com.codewithsvns.foodapp.ml.MlApiClient;
import com.codewithsvns.foodapp.ml.dto.DeliveryPredictionRequest;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer/ml")
public class MlDeliveryPredictionController {

    private final MlApiClient mlApiClient;

    public MlDeliveryPredictionController(MlApiClient mlApiClient) {
        this.mlApiClient = mlApiClient;
    }

    @PostMapping("/predict-delivery")
    public JsonNode predictDelivery(
            @RequestBody DeliveryPredictionRequest request) {
        return mlApiClient.predictDelivery(request);
    }
}