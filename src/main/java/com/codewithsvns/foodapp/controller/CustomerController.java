
package com.codewithsvns.foodapp.controller;

import com.codewithsvns.foodapp.dto.UserProfileResponse;
import com.codewithsvns.foodapp.model.UserRole;
import com.codewithsvns.foodapp.service.RegistrationService;
import com.codewithsvns.foodapp.service.UserProfileService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/customer")
public class CustomerController {

    private final UserProfileService userProfileService;
    private final RegistrationService registrationService;

    public CustomerController(
            UserProfileService userProfileService,
            RegistrationService registrationService) {
        this.userProfileService = userProfileService;
        this.registrationService = registrationService;
    }

    @GetMapping("/me")
    public UserProfileResponse getMyProfile(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication is required.");
        }

        UUID userId;
        try {
            userId = UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid authenticated user identifier.");
        }

        Map<String, Object> session =
                Map.of("user", Map.of("id", userId.toString()));

        registrationService.requireActiveProfile(session);
        registrationService.requireRole(session, UserRole.CUSTOMER);

        return userProfileService.getProfile(userId);
    }
}
