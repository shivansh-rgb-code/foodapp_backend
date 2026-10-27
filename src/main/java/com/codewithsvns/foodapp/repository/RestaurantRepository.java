package com.codewithsvns.foodapp.repository;

import com.codewithsvns.foodapp.model.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RestaurantRepository
        extends JpaRepository<Restaurant, UUID> {

    Optional<Restaurant> findByOwner_Id(UUID ownerId);

    @Query("select r from Restaurant r where r.status = com.codewithsvns.foodapp.model.RestaurantStatus.APPROVED "
            + "and (:openOnly is null or r.open = :openOnly) "
            + "and (lower(r.name) like lower(concat('%', :query, '%')) "
            + "or lower(r.address) like lower(concat('%', :query, '%')) "
            + "or lower(coalesce(r.cuisine, '')) like lower(concat('%', :query, '%')) "
            + "or :query is null) "
            + "order by r.name")
    List<Restaurant> searchApproved(
            @Param("query") String query,
            @Param("openOnly") Boolean openOnly);
}
