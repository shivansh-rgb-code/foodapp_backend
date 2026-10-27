package com.codewithsvns.foodapp.service;

import com.codewithsvns.foodapp.model.AccountStatus;
import com.codewithsvns.foodapp.model.DeliveryPartner;
import com.codewithsvns.foodapp.model.PendingRegistration;
import com.codewithsvns.foodapp.model.Restaurant;
import com.codewithsvns.foodapp.model.UserProfile;
import com.codewithsvns.foodapp.model.UserRole;
import com.codewithsvns.foodapp.repository.DeliveryPartnerRepository;
import com.codewithsvns.foodapp.repository.PendingRegistrationRepository;
import com.codewithsvns.foodapp.repository.RestaurantRepository;
import com.codewithsvns.foodapp.repository.UserProfileRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

@Service
public class RegistrationService {

    private final SupabaseAuthService authService;
    private final PendingRegistrationRepository pendingRepository;
    private final UserProfileRepository profileRepository;
    private final RestaurantRepository restaurantRepository;
    private final DeliveryPartnerRepository deliveryPartnerRepository;

    public RegistrationService(
            SupabaseAuthService authService,
            PendingRegistrationRepository pendingRepository,
            UserProfileRepository profileRepository,
            RestaurantRepository restaurantRepository,
            DeliveryPartnerRepository deliveryPartnerRepository) {

        this.authService = authService;
        this.pendingRepository = pendingRepository;
        this.profileRepository = profileRepository;
        this.restaurantRepository = restaurantRepository;
        this.deliveryPartnerRepository = deliveryPartnerRepository;
    }

    public void signUp(
            String name,
            String email,
            String password,
            UserRole role,
            String restaurantName,
            String restaurantAddress) {

        String normalizedEmail = email.trim().toLowerCase();
        String normalizedName = name.trim();


        Map<String, Object> result = authService.signUp(
                normalizedName,
                normalizedEmail,
                password
        );

        if (result == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "The authentication service returned an empty response."
            );
        }

        Object userObject = result.get("user");
        Map<?, ?> user;

        if (userObject instanceof Map<?, ?> nestedUser) {
            user = nestedUser;
        } else if (result.get("id") != null && result.get("email") != null) {
            user = result;
        } else {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Could not safely associate the signup with a user. Please retry or contact support."
            );
        }

        if (user.get("id") == null || user.get("email") == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "The authentication service returned incomplete user information."
            );
        }

        UUID userId;
        try {
            userId = UUID.fromString(String.valueOf(user.get("id")));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "The authentication service returned an invalid user identifier."
            );
        }

        String returnedEmail = String.valueOf(user.get("email"))
                .trim()
                .toLowerCase();

        if (!normalizedEmail.equals(returnedEmail)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "The authentication service returned an unexpected email address."
            );
        }


        pendingRepository.findByEmail(normalizedEmail).ifPresent(pending -> {
            if (pending.isExpired()) {
                pendingRepository.delete(pending);
            } else {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "A registration is already pending for this email. Verify it or wait for it to expire."
                );
            }
        });


        pendingRepository.save(new PendingRegistration(
                normalizedEmail,
                userId,
                normalizedName,
                role,
                restaurantName,
                restaurantAddress
        ));
    }

    @Transactional
    public void completeVerifiedRegistration(Map<String, Object> session) {
        Object userObject = session.get("user");

        if (!(userObject instanceof Map<?, ?> user)
                || user.get("id") == null
                || user.get("email") == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Verified user information is missing."
            );
        }

        UUID userId;
        try {
            userId = UUID.fromString(String.valueOf(user.get("id")));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid verified user identifier."
            );
        }

        String email = String.valueOf(user.get("email"))
                .trim()
                .toLowerCase();

        PendingRegistration pending = pendingRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "No pending registration was found. Please sign up again."
                ));

        if (!pending.getSupabaseUserId().equals(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "This registration does not match the verified user."
            );
        }

        if (pending.isExpired()) {
            pendingRepository.delete(pending);
            throw new ResponseStatusException(
                    HttpStatus.GONE,
                    "This registration has expired. Please sign up again."
            );
        }

        if (profileRepository.existsById(userId)) {
            pendingRepository.delete(pending);
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A profile already exists for this account."
            );
        }

        UserProfile profile = profileRepository.save(new UserProfile(
                userId,
                pending.getFullName(),
                pending.getRole(),
                AccountStatus.ACTIVE
        ));

        if (pending.getRole() == UserRole.RESTAURANT) {
            restaurantRepository.save(new Restaurant(
                    profile,
                    pending.getRestaurantName(),
                    pending.getRestaurantAddress()
            ));
        } else if (pending.getRole() == UserRole.DELIVERY_PARTNER) {
            deliveryPartnerRepository.save(new DeliveryPartner(profile));
        }

        pendingRepository.delete(pending);
    }

    public void requireRole(
            Map<String, Object> session,
            UserRole expectedRole) {

        Object userObject = session.get("user");

        if (!(userObject instanceof Map<?, ?> user)
                || user.get("id") == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid authentication session."
            );
        }

        UUID userId;
        try {
            userId = UUID.fromString(String.valueOf(user.get("id")));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid authentication session."
            );
        }

        UserProfile profile = profileRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "This account has not completed registration."
                ));

        if (profile.getRole() != expectedRole) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "This account cannot log in through this account type."
            );
        }

        if (profile.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "This account is not active."
            );
        }
    }

    public void requireActiveProfile(Map<String, Object> session) {
        Map<?, ?> user = getSessionUser(session);

        UUID userId;
        try {
            userId = UUID.fromString(String.valueOf(user.get("id")));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid user session."
            );
        }

        UserProfile profile = profileRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Registration is incomplete. Please complete signup first."
                ));

        if (profile.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "This account is not active."
            );
        }
    }

    private Map<?, ?> getSessionUser(Map<String, Object> session) {
        Object userObject = session.get("user");

        if (!(userObject instanceof Map<?, ?> user)
                || user.get("id") == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid user session."
            );
        }

        return user;
    }
}