package com.codewithsvns.foodapp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "delivery_addresses")
public class DeliveryAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserProfile user;

    @Column(length = 50)
    private String label;

    @Column(name = "full_address", nullable = false, length = 500)
    private String fullAddress;

    @Column(length = 255)
    private String street;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String state;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    private Double latitude;

    private Double longitude;

    @Column(name = "delivery_instructions", length = 500)
    private String deliveryInstructions;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected DeliveryAddress() {
    }

    public DeliveryAddress(
            UserProfile user,
            String label,
            String fullAddress,
            String street,
            String city,
            String state,
            String postalCode,
            Double latitude,
            Double longitude,
            String deliveryInstructions,
            boolean isDefault) {
        this.user = user;
        this.label = label;
        this.fullAddress = fullAddress;
        this.street = street;
        this.city = city;
        this.state = state;
        this.postalCode = postalCode;
        this.latitude = latitude;
        this.longitude = longitude;
        this.deliveryInstructions = deliveryInstructions;
        this.isDefault = isDefault;
    }

    @jakarta.persistence.PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        updatedAt = Instant.now();
    }

    @jakarta.persistence.PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UserProfile getUser() {
        return user;
    }

    public String getLabel() {
        return label;
    }

    public String getFullAddress() {
        return fullAddress;
    }

    public String getStreet() {
        return street;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public String getDeliveryInstructions() {
        return deliveryInstructions;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setFullAddress(String fullAddress) {
        this.fullAddress = fullAddress;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public void setDeliveryInstructions(String deliveryInstructions) {
        this.deliveryInstructions = deliveryInstructions;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }
}
