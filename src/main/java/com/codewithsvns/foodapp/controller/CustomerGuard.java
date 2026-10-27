package com.codewithsvns.foodapp.controller;

import com.codewithsvns.foodapp.model.UserRole;
import com.codewithsvns.foodapp.service.RegistrationService;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

@Component
public class CustomerGuard {

    private final RegistrationService registrationService;

    public CustomerGuard(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    public UUID requireCustomer(Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Authentication is required.");
        }

        UUID userId;
        try {
            userId = UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Invalid authenticated user identifier.");
        }

        Map<String, Object> session =
                Map.of("user", Map.of("id", userId.toString()));

        registrationService.requireActiveProfile(session);
        registrationService.requireRole(session, UserRole.CUSTOMER);

        return userId;
    }
}
