
package com.codewithsvns.foodapp.controller;

import com.codewithsvns.foodapp.dto.UserProfileCreateRequest;
import com.codewithsvns.foodapp.dto.UserProfileResponse;
import com.codewithsvns.foodapp.service.UserProfileService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/profiles")
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping("/me")
    public UserProfileResponse getMyProfile(
            @AuthenticationPrincipal Jwt jwt) {

        UUID userId = UUID.fromString(jwt.getSubject());

        return userProfileService.getProfile(userId);
    }

    @PutMapping("/me")
    public UserProfileResponse updateMyProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UserProfileCreateRequest request) {

        UUID userId = UUID.fromString(jwt.getSubject());

        return userProfileService.updateProfile(
                userId,
                request.fullName()
        );
    }
    @PostMapping("/me")
    public UserProfileResponse createMyProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UserProfileCreateRequest request) {

        UUID userId = UUID.fromString(jwt.getSubject());

        try {
            return userProfileService.createProfile(
                    userId,
                    request.fullName()
            );
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Could not create profile because of a data conflict"
            );
        }
    }
}
