
package com.codewithsvns.foodapp.repository;

import com.codewithsvns.foodapp.model.FoodCategory;
import com.codewithsvns.foodapp.model.FoodItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FoodItemRepository extends JpaRepository<FoodItem, UUID> {

    List<FoodItem> findByRestaurant_Id(UUID restaurantId);

    List<FoodItem> findByRestaurant_IdAndAvailableTrue(UUID restaurantId);

    List<FoodItem> findByRestaurant_IdAndCategoryAndAvailableTrue(
            UUID restaurantId,
            FoodCategory category);
}
