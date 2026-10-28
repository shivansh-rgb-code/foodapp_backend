package com.codewithsvns.foodapp.service;

import com.codewithsvns.foodapp.model.AccountStatus;
import com.codewithsvns.foodapp.model.RestaurantStatus;
import com.codewithsvns.foodapp.repository.DeliveryAddressRepository;
import com.codewithsvns.foodapp.repository.DeliveryPartnerRepository;
import com.codewithsvns.foodapp.repository.RestaurantRepository;
import com.codewithsvns.foodapp.repository.UserProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AccountDeletionService {

    private final UserProfileRepository userProfileRepository;
    private final RestaurantRepository restaurantRepository;
    private final DeliveryPartnerRepository deliveryPartnerRepository;
    private final DeliveryAddressRepository deliveryAddressRepository;
    private final SupabaseAuthService authService;

    public AccountDeletionService(
            UserProfileRepository userProfileRepository,
            RestaurantRepository restaurantRepository,
            DeliveryPartnerRepository deliveryPartnerRepository,
            DeliveryAddressRepository deliveryAddressRepository,
            SupabaseAuthService authService) {
        this.userProfileRepository = userProfileRepository;
        this.restaurantRepository = restaurantRepository;
        this.deliveryPartnerRepository = deliveryPartnerRepository;
        this.deliveryAddressRepository = deliveryAddressRepository;
        this.authService = authService;
    }

    @Transactional
    public void deleteAccount(UUID userId) {
        var profile = userProfileRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Account profile not found."));

        if (profile.getAccountStatus() != AccountStatus.DELETED) {
            profile.markDeleted();

            restaurantRepository.findByOwner_Id(userId).ifPresent(restaurant -> {
                restaurant.setOpen(false);
                restaurant.setStatus(RestaurantStatus.SUSPENDED);
            });

            deliveryPartnerRepository.findByUserProfile_Id(userId)
                    .ifPresent(partner -> partner.setAvailable(false));

            deliveryAddressRepository.deleteByUser_Id(userId);

            userProfileRepository.save(profile);
        }

        // Also supports retry if the profile was already marked deleted.
        authService.deleteAuthUser(userId);
    }
}