
package com.codewithsvns.foodapp.ml.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DeliveryPredictionRequest(
        @JsonProperty("Delivery_person_Age")
        Double deliveryPersonAge,

        @JsonProperty("Delivery_person_Ratings")
        Double deliveryPersonRatings,

        @JsonProperty("Restaurant_latitude")
        Double restaurantLatitude,

        @JsonProperty("Restaurant_longitude")
        Double restaurantLongitude,

        @JsonProperty("Delivery_location_latitude")
        Double deliveryLocationLatitude,

        @JsonProperty("Delivery_location_longitude")
        Double deliveryLocationLongitude,

        @JsonProperty("Vehicle_condition")
        Integer vehicleCondition,

        @JsonProperty("distance_km")
        Double distanceKm,

        @JsonProperty("order_hour")
        Integer orderHour,

        @JsonProperty("order_minute")
        Integer orderMinute,

        @JsonProperty("order_weekday")
        Integer orderWeekday,

        @JsonProperty("order_month")
        Integer orderMonth,

        @JsonProperty("Time_Orderd")
        String timeOrdered,

        @JsonProperty("Order_Date")
        String orderDate,

        @JsonProperty("Weatherconditions")
        String weatherConditions,

        @JsonProperty("Road_traffic_density")
        String roadTrafficDensity,

        @JsonProperty("Type_of_order")
        String typeOfOrder,

        @JsonProperty("Type_of_vehicle")
        String typeOfVehicle,

        @JsonProperty("Festival")
        String festival,

        @JsonProperty("City")
        String city
) {}
