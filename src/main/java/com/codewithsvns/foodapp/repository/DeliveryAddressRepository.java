package com.codewithsvns.foodapp.repository;

import com.codewithsvns.foodapp.model.DeliveryAddress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryAddressRepository extends JpaRepository<DeliveryAddress, UUID> {

    List<DeliveryAddress> findByUser_IdOrderByCreatedAtDesc(UUID userId);

    Optional<DeliveryAddress> findByIdAndUser_Id(UUID id, UUID userId);

    Optional<DeliveryAddress> findByUser_IdAndIsDefaultTrue(UUID userId);

    void deleteByUser_Id(UUID userId);

    long countByUser_Id(UUID userId);
}
