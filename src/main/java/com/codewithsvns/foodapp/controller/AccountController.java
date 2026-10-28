package com.codewithsvns.foodapp.controller;

import com.codewithsvns.foodapp.dto.UpdatePasswordRequest;
import com.codewithsvns.foodapp.service.SupabaseAuthService;
import jakarta.validation.Valid;
import com.codewithsvns.foodapp.service.AccountDeletionService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.security.oauth2.jwt.Jwt;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final SupabaseAuthService authService;
    private final AccountDeletionService deletionService;

    public AccountController(
            SupabaseAuthService authService,
            AccountDeletionService deletionService) {
        this.authService = authService;
        this.deletionService = deletionService;
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMyAccount(
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        deletionService.deleteAccount(userId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/password")
    public ResponseEntity<Map<String, String>> changePassword(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdatePasswordRequest request) {

        authService.updatePassword(jwt.getTokenValue(), request.newPassword());
        return ResponseEntity.ok(Map.of("message", "Password updated successfully."));
    }

    @ExceptionHandler(SupabaseAuthService.AuthException.class)
    public ResponseEntity<Map<String, String>> handleAuthException(
            SupabaseAuthService.AuthException ex) {
        return ResponseEntity.status(ex.getStatus()).body(Map.of("message", ex.getMessage()));
    }
}