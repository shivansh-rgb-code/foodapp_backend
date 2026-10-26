package com.codewithsvns.foodapp.service;

import com.codewithsvns.foodapp.dto.DeliveryAddressRequest;
import com.codewithsvns.foodapp.dto.DeliveryAddressResponse;
import com.codewithsvns.foodapp.model.DeliveryAddress;
import com.codewithsvns.foodapp.model.UserProfile;
import com.codewithsvns.foodapp.repository.DeliveryAddressRepository;
import com.codewithsvns.foodapp.repository.UserProfileRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class DeliveryAddressService {

    private static final int MAX_ADDRESSES_PER_USER = 20;

    private final DeliveryAddressRepository addressRepository;
    private final UserProfileRepository userProfileRepository;

    public DeliveryAddressService(
            DeliveryAddressRepository addressRepository,
            UserProfileRepository userProfileRepository) {
        this.addressRepository = addressRepository;
        this.userProfileRepository = userProfileRepository;
    }

    @Transactional
    public DeliveryAddressResponse create(UUID userId, DeliveryAddressRequest request) {
        long count = addressRepository.countByUser_Id(userId);
        if (count >= MAX_ADDRESSES_PER_USER) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Maximum of " + MAX_ADDRESSES_PER_USER + " addresses allowed per user.");
        }

        UserProfile user = userProfileRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User profile not found."));

        boolean makeDefault = Boolean.TRUE.equals(request.isDefault());

        if (makeDefault) {
            clearExistingDefault(userId);
        }

        if (count == 0 && !makeDefault) {
            makeDefault = true;
        }

        DeliveryAddress address = new DeliveryAddress(
                user,
                request.label(),
                request.fullAddress(),
                request.street(),
                request.city(),
                request.state(),
                request.postalCode(),
                request.latitude(),
                request.longitude(),
                request.deliveryInstructions(),
                makeDefault);

        address = addressRepository.save(address);
        return toResponse(address);
    }

    @Transactional
    public DeliveryAddressResponse update(UUID userId, UUID addressId, DeliveryAddressRequest request) {
        DeliveryAddress address = addressRepository.findByIdAndUser_Id(addressId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Address not found."));

        address.setLabel(request.label());
        address.setFullAddress(request.fullAddress());
        address.setStreet(request.street());
        address.setCity(request.city());
        address.setState(request.state());
        address.setPostalCode(request.postalCode());
        address.setLatitude(request.latitude());
        address.setLongitude(request.longitude());
        address.setDeliveryInstructions(request.deliveryInstructions());

        if (Boolean.TRUE.equals(request.isDefault()) && !address.isDefault()) {
            clearExistingDefault(userId);
            address.setDefault(true);
        }

        address = addressRepository.save(address);
        return toResponse(address);
    }

    @Transactional(readOnly = true)
    public List<DeliveryAddressResponse> list(UUID userId) {
        return addressRepository.findByUser_IdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DeliveryAddressResponse getById(UUID userId, UUID addressId) {
        DeliveryAddress address = addressRepository.findByIdAndUser_Id(addressId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Address not found."));
        return toResponse(address);
    }

    @Transactional
    public void delete(UUID userId, UUID addressId) {
        DeliveryAddress address = addressRepository.findByIdAndUser_Id(addressId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Address not found."));
        boolean wasDefault = address.isDefault();
        addressRepository.delete(address);

        if (wasDefault) {
            addressRepository.findByUser_IdOrderByCreatedAtDesc(userId).stream()
                    .findFirst()
                    .ifPresent(replacement -> {
                        replacement.setDefault(true);
                        addressRepository.save(replacement);
                    });
        }
    }

    @Transactional
    public DeliveryAddressResponse setDefault(UUID userId, UUID addressId) {
        DeliveryAddress address = addressRepository.findByIdAndUser_Id(addressId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Address not found."));

        if (!address.isDefault()) {
            clearExistingDefault(userId);
            address.setDefault(true);
            address = addressRepository.save(address);
        }

        return toResponse(address);
    }

    private void clearExistingDefault(UUID userId) {
        addressRepository.findByUser_IdAndIsDefaultTrue(userId)
                .ifPresent(existing -> {
                    existing.setDefault(false);
                    addressRepository.save(existing);
                });
    }

    private DeliveryAddressResponse toResponse(DeliveryAddress address) {
        return new DeliveryAddressResponse(
                address.getId(),
                address.getLabel(),
                address.getFullAddress(),
                address.getStreet(),
                address.getCity(),
                address.getState(),
                address.getPostalCode(),
                address.getLatitude(),
                address.getLongitude(),
                address.getDeliveryInstructions(),
                address.isDefault(),
                address.getCreatedAt());
    }
}
