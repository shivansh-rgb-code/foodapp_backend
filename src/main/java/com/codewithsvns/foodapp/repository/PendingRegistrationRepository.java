
package com.codewithsvns.foodapp.repository;

import com.codewithsvns.foodapp.model.PendingRegistration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PendingRegistrationRepository
        extends JpaRepository<PendingRegistration, String> {

    Optional<PendingRegistration> findByEmail(String email);
}
