
package com.codewithsvns.foodapp.repository;

import com.codewithsvns.foodapp.model.DeliveryPartner;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface DeliveryPartnerRepository extends JpaRepository<DeliveryPartner, UUID> {

    Optional<DeliveryPartner> findByUserProfile_Id(UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from DeliveryPartner d where d.userProfile.id = :userId")
    Optional<DeliveryPartner> findByUserProfileIdForUpdate(@Param("userId") UUID userId);
}
