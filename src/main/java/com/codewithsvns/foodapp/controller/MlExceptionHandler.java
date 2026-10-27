package com.codewithsvns.foodapp.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

@RestControllerAdvice(assignableTypes = {
        MlRecommendationController.class,
        MlPreviousOrderRecommendationController.class,
        MlDeliveryPredictionController.class
})
public class MlExceptionHandler {


    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<Map<String, String>> mlUnreachable(ResourceAccessException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "message",
                "The recommendation service is waking up or unavailable. Please try again in a minute."));
    }


    @ExceptionHandler(RestClientResponseException.class)
    public ResponseEntity<Map<String, String>> mlRejected(RestClientResponseException ex) {
        HttpStatus status = ex.getStatusCode().is4xxClientError()
                ? HttpStatus.BAD_REQUEST
                : HttpStatus.BAD_GATEWAY;

        return ResponseEntity.status(status).body(Map.of(
                "message", "The recommendation service could not process this request."));
    }
}
