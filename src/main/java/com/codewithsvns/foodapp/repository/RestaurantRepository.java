
package com.codewithsvns.foodapp.repository;

import com.codewithsvns.foodapp.model.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RestaurantRepository
        extends JpaRepository<Restaurant, UUID> {

    Optional<Restaurant> findByOwner_Id(UUID ownerId);
}
