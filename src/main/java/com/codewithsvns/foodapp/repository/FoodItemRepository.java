
package com.codewithsvns.foodapp.repository;

import com.codewithsvns.foodapp.model.FoodCategory;
import com.codewithsvns.foodapp.model.FoodItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface FoodItemRepository extends JpaRepository<FoodItem, UUID> {

    List<FoodItem> findByRestaurant_Id(UUID restaurantId);

    List<FoodItem> findByRestaurant_IdAndAvailableTrue(UUID restaurantId);

    List<FoodItem> findByRestaurant_IdAndCategoryAndAvailableTrue(
            UUID restaurantId,
            FoodCategory category);

    @Query("select f from FoodItem f where f.restaurant.id = :restaurantId "
            + "and f.available = true "
            + "and (:category is null or f.category = :category) "
            + "and (:query is null or lower(f.name) like lower(concat('%', :query, '%')) "
            + "or lower(coalesce(f.description, '')) like lower(concat('%', :query, '%'))) "
            + "order by f.category, f.name")
    List<FoodItem> searchAvailable(
            @Param("restaurantId") UUID restaurantId,
            @Param("category") FoodCategory category,
            @Param("query") String query);
}
