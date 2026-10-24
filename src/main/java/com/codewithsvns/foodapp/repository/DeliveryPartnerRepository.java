
package com.codewithsvns.foodapp.repository;

import com.codewithsvns.foodapp.model.DeliveryPartner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DeliveryPartnerRepository
        extends JpaRepository<DeliveryPartner, UUID> {

    Optional<DeliveryPartner> findByUserProfile_Id(UUID userId);
}
