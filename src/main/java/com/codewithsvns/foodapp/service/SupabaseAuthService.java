package com.codewithsvns.foodapp.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Service
public class SupabaseAuthService {

    public static final String ACCESS_COOKIE = "sb-access-token";
    public static final String REFRESH_COOKIE = "sb-refresh-token";
    public static final String VERIFIER_COOKIE = "pkce-verifier";

    private final RestClient supabase;
    private final UserProfileService userProfileService;
    private final String supabaseUrl;
    private final String backendUrl;
    private final String passwordResetRedirectUrl;

    public SupabaseAuthService(
            UserProfileService userProfileService,
            @Value("${supabase.url}") String supabaseUrl,
            @Value("${supabase.anon-key}") String anonKey,
            @Value("${app.backend-url}") String backendUrl,
            @Value("${app.password-reset-redirect-url}") String passwordResetRedirectUrl) {

        this.userProfileService = userProfileService;
        this.supabaseUrl = stripTrailingSlash(supabaseUrl);
        this.backendUrl = stripTrailingSlash(backendUrl);
        this.passwordResetRedirectUrl = passwordResetRedirectUrl;


        this.supabase = RestClient.builder()
                .baseUrl(this.supabaseUrl)
                .defaultHeader("apikey", anonKey)
                .build();
    }


    public void updatePassword(String accessToken, String newPassword) {
        try {
            supabase.put()
                    .uri("/auth/v1/user")
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("password", newPassword))
                    .retrieve()
                    .toBodilessEntity();

        } catch (RestClientResponseException ex) {
            throw translateError(ex);

        } catch (RestClientException ex) {
            throw new AuthException(
                    HttpStatus.BAD_GATEWAY,
                    "Authentication service is unreachable. Please try again."
            );
        }
    }


    public Map<String, Object> signUp(String name, String email, String password) {
        return post("/auth/v1/signup", Map.of(
                "email", normalizeEmail(email),
                "password", password,
                "data", Map.of("full_name", name)));
    }

    public Map<String, Object> logIn(String email, String password) {
        return post("/auth/v1/token?grant_type=password", Map.of(
                "email", normalizeEmail(email),
                "password", password));
    }

    public Map<String, Object> refresh(String refreshToken) {
        return post("/auth/v1/token?grant_type=refresh_token", Map.of(
                "refresh_token", refreshToken));
    }

    public void sendPasswordReset(String email, String codeChallenge) {
        post("/auth/v1/recover?redirect_to={redirectTo}",
                Map.of("email", normalizeEmail(email),
                        "code_challenge", codeChallenge,
                        "code_challenge_method", "s256"),
                passwordResetRedirectUrl);
    }




    public String generateCodeVerifier() {
        byte[] randomBytes = new byte[64];
        new SecureRandom().nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }


    public String toCodeChallenge(String verifier) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(verifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }


    public String buildSocialLoginUrl(String provider, String codeChallenge) {
        return UriComponentsBuilder
                .fromUriString(supabaseUrl + "/auth/v1/authorize")
                .queryParam("provider", provider)
                .queryParam("redirect_to", backendUrl + "/api/auth/callback")
                .queryParam("code_challenge", codeChallenge)
                .queryParam("code_challenge_method", "s256")
                .build()
                .encode()
                .toUriString();
    }


    public Map<String, Object> exchangeCode(String code, String verifier) {
        return post("/auth/v1/token?grant_type=pkce", Map.of(
                "auth_code", code,
                "code_verifier", verifier));
    }


    public void createProfileIfMissing(Map<String, Object> session) {
        Map<?, ?> user = (Map<?, ?>) session.get("user");
        UUID userId = UUID.fromString(String.valueOf(user.get("id")));

        try {
            userProfileService.createProfile(userId, pickFullName(user));
        } catch (DataIntegrityViolationException ex) {
            // Two requests created it at the same moment - the row exists, so that's fine.
        }
    }

    private String pickFullName(Map<?, ?> user) {
        String name = null;

        if (user.get("user_metadata") instanceof Map<?, ?> metadata) {
            name = firstNonBlank(metadata.get("full_name"), metadata.get("name"));
        }
        if (name == null && user.get("email") != null) {
            String email = String.valueOf(user.get("email"));
            name = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
        }
        if (name == null || name.isBlank()) {
            name = "Customer";
        }

        name = name.trim();
        return name.length() > 100 ? name.substring(0, 100) : name;
    }

    private String firstNonBlank(Object first, Object second) {
        if (first != null && !String.valueOf(first).isBlank()) {
            return String.valueOf(first);
        }
        if (second != null && !String.valueOf(second).isBlank()) {
            return String.valueOf(second);
        }
        return null;
    }



    private Map<String, Object> post(String path, Object body, Object... uriVariables) {
        try {
            return supabase.post()
                    .uri(path, uriVariables)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        } catch (RestClientResponseException ex) {
            throw translateError(ex);
        } catch (RestClientException ex) {
            throw new AuthException(HttpStatus.BAD_GATEWAY,
                    "Authentication service is unreachable. Please try again.");
        }
    }

    private AuthException translateError(RestClientResponseException ex) {
        String errorCode = "";
        String supabaseMessage = "";

        try {
            Map<?, ?> body = ex.getResponseBodyAs(Map.class);
            if (body != null) {
                errorCode = String.valueOf(body.containsKey("error_code") ? body.get("error_code") : "");
                Object msg = body.get("msg") != null ? body.get("msg") : body.get("message");
                supabaseMessage = msg == null ? "" : String.valueOf(msg);
            }
        } catch (Exception ignored) {
            // Body was not JSON - fall back to the generic messages below.
        }

        int status = ex.getStatusCode().value();

        if (errorCode.equals("invalid_credentials")) {
            return new AuthException(HttpStatus.UNAUTHORIZED, "Invalid email or password.");
        }
        if (errorCode.equals("email_not_confirmed")) {
            return new AuthException(HttpStatus.FORBIDDEN,
                    "Please verify your email address before logging in.");
        }
        if (errorCode.equals("user_already_exists") || errorCode.equals("email_exists")) {
            return new AuthException(HttpStatus.CONFLICT,
                    "An account with this email already exists.");
        }
        if (status == 429) {
            return new AuthException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many attempts. Please wait a moment and try again.");
        }
        if (status >= 500) {
            return new AuthException(HttpStatus.BAD_GATEWAY,
                    "Authentication service error. Please try again.");
        }
        if (errorCode.equals("weak_password") && !supabaseMessage.isBlank()) {
            return new AuthException(HttpStatus.BAD_REQUEST, supabaseMessage);
        }
        return new AuthException(HttpStatus.BAD_REQUEST, "Request could not be completed.");
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }


    public static class AuthException extends RuntimeException {
        private final HttpStatus status;

        public AuthException(HttpStatus status, String message) {
            super(message);
            this.status = status;
        }

        public HttpStatus getStatus() {
            return status;
        }
    }
}