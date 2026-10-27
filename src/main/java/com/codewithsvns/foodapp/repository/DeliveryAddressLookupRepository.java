package com.codewithsvns.foodapp.repository;

import com.codewithsvns.foodapp.model.DeliveryAddress;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface DeliveryAddressLookupRepository extends Repository<DeliveryAddress, UUID> {

    interface Coordinates {
        Double getLatitude();
        Double getLongitude();
    }

    @Query("select a.latitude as latitude, a.longitude as longitude "
            + "from DeliveryAddress a "
            + "where a.user.id = :userId "
            + "and a.latitude is not null and a.longitude is not null "
            + "and lower(a.fullAddress) = lower(:address) "
            + "order by a.updatedAt desc")
    List<Coordinates> findCoordinates(
            @Param("userId") UUID userId,
            @Param("address") String address,
            Pageable pageable);
}
