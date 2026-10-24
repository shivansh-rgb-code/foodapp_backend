
package com.codewithsvns.foodapp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pending_registrations")
public class PendingRegistration {

    @Id
    @Column(nullable = false, length = 254)
    private String email;

    @Column(name = "supabase_user_id", nullable = false, unique = true)
    private UUID supabaseUserId;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserRole role;

    @Column(name = "restaurant_name", length = 150)
    private String restaurantName;

    @Column(name = "restaurant_address", length = 255)
    private String restaurantAddress;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected PendingRegistration() {
    }

    public PendingRegistration(
            String email,
            UUID supabaseUserId,
            String fullName,
            UserRole role,
            String restaurantName,
            String restaurantAddress) {

        this.email = email.trim().toLowerCase();
        this.supabaseUserId = supabaseUserId;
        this.fullName = fullName;
        this.role = role;
        this.restaurantName = restaurantName;
        this.restaurantAddress = restaurantAddress;
    }

    @jakarta.persistence.PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (expiresAt == null) {
            expiresAt = Instant.now().plusSeconds(86400);
        }
    }

    public String getEmail() {
        return email;
    }

    public UUID getSupabaseUserId() {
        return supabaseUserId;
    }

    public String getFullName() {
        return fullName;
    }

    public UserRole getRole() {
        return role;
    }

    public String getRestaurantName() {
        return restaurantName;
    }

    public String getRestaurantAddress() {
        return restaurantAddress;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
