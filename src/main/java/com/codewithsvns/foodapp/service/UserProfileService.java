
package com.codewithsvns.foodapp.service;

import com.codewithsvns.foodapp.dto.UserProfileResponse;
import com.codewithsvns.foodapp.model.AccountStatus;
import com.codewithsvns.foodapp.model.UserProfile;
import com.codewithsvns.foodapp.model.UserRole;
import com.codewithsvns.foodapp.repository.UserProfileRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;

    public UserProfileService(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    public UserProfileResponse createProfile(UUID userId, String fullName) {
        UserProfile profile = userProfileRepository.findById(userId)
                .orElseGet(() -> userProfileRepository.save(
                        new UserProfile(
                                userId,
                                fullName,
                                UserRole.CUSTOMER,
                                AccountStatus.ACTIVE
                        )
                ));

        return toResponse(profile);
    }


    public UserProfileResponse updateProfile(UUID userId, String fullName) {
        UserProfile profile = userProfileRepository.findById(userId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "User profile not found"
                ));

        profile.updateFullName(fullName.trim());

        return toResponse(userProfileRepository.save(profile));
    }

    public UserProfileResponse getProfile(UUID userId) {
        UserProfile profile = userProfileRepository.findById(userId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "User profile not found"
                ));

        return toResponse(profile);
    }


    private UserProfileResponse toResponse(UserProfile profile) {
        return new UserProfileResponse(
                profile.getId(),
                profile.getFullName(),
                profile.getRole(),
                profile.getAccountStatus(),
                profile.getCreatedAt()
        );
    }
}
