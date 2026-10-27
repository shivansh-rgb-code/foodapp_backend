package com.codewithsvns.foodapp.controller;

import com.codewithsvns.foodapp.dto.AuthLoginRequest;
import com.codewithsvns.foodapp.dto.AuthSignupRequest;
import com.codewithsvns.foodapp.dto.DeliveryPartnerSignupRequest;
import com.codewithsvns.foodapp.dto.ForgotPasswordRequest;
import com.codewithsvns.foodapp.dto.ResendOtpRequest;
import com.codewithsvns.foodapp.dto.RestaurantSignupRequest;
import com.codewithsvns.foodapp.dto.UpdatePasswordRequest;
import com.codewithsvns.foodapp.dto.VerifyOtpRequest;
import com.codewithsvns.foodapp.model.UserRole;
import com.codewithsvns.foodapp.service.RegistrationService;
import com.codewithsvns.foodapp.service.SupabaseAuthService;
import com.codewithsvns.foodapp.service.SupabaseAuthService.AuthException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log =
            LoggerFactory.getLogger(AuthController.class);

    private static final long REFRESH_COOKIE_SECONDS = 60L * 60 * 24 * 30;
    private static final long VERIFIER_COOKIE_SECONDS = 600;
    private static final String AUTH_PATH = "/api/auth";

    private final RegistrationService registrationService;
    private final SupabaseAuthService authService;
    private final String frontendUrl;
    private final boolean cookieSecure;
    private final String cookieSameSite;

    public AuthController(
            SupabaseAuthService authService,
            RegistrationService registrationService,
            @Value("${app.frontend-url}") String frontendUrl,
            @Value("${app.cookie.secure}") boolean cookieSecure,
            @Value("${app.cookie.same-site}") String cookieSameSite) {

        this.authService = authService;
        this.registrationService = registrationService;
        this.frontendUrl = frontendUrl.endsWith("/")
                ? frontendUrl.substring(0, frontendUrl.length() - 1)
                : frontendUrl;
        this.cookieSecure = cookieSecure;
        this.cookieSameSite = cookieSameSite;
    }

    @PostMapping("/signup")
    public ResponseEntity<Map<String, String>> signUp(
            @Valid @RequestBody AuthSignupRequest request) {

        registrationService.signUp(
                request.name(),
                request.email(),
                request.password(),
                UserRole.CUSTOMER,
                null,
                null
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message",
                "Account created. Please check your email for the verification code.",
                "email", request.email().trim().toLowerCase()
        ));
    }

    @PostMapping("/customer/signup")
    public ResponseEntity<Map<String, String>> customerSignup(
            @Valid @RequestBody AuthSignupRequest request) {

        registrationService.signUp(
                request.name(),
                request.email(),
                request.password(),
                UserRole.CUSTOMER,
                null,
                null
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message",
                "Account created. Please check your email for the verification code.",
                "email", request.email().trim().toLowerCase()
        ));
    }

    @PostMapping("/restaurant/signup")
    public ResponseEntity<Map<String, String>> restaurantSignup(
            @Valid @RequestBody RestaurantSignupRequest request) {

        registrationService.signUp(
                request.name(),
                request.email(),
                request.password(),
                UserRole.RESTAURANT,
                request.restaurantName().trim(),
                request.restaurantAddress().trim()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Restaurant account created. Please verify your email.",
                "email", request.email().trim().toLowerCase()
        ));
    }

    @PostMapping("/delivery/signup")
    public ResponseEntity<Map<String, String>> deliverySignup(
            @Valid @RequestBody DeliveryPartnerSignupRequest request) {

        registrationService.signUp(
                request.name(),
                request.email(),
                request.password(),
                UserRole.DELIVERY_PARTNER,
                null,
                null
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Delivery partner account created. Please verify your email.",
                "email", request.email().trim().toLowerCase()
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> logIn(
            @Valid @RequestBody AuthLoginRequest request,
            HttpServletResponse response) {

        Map<String, Object> session =
                authService.logIn(request.email(), request.password());

        registrationService.requireRole(session, UserRole.CUSTOMER);
        startSession(response, session);

        return ResponseEntity.ok(
                Map.of("message", "Login successful."));
    }

    @PostMapping("/customer/login")
    public ResponseEntity<Map<String, String>> customerLogin(
            @Valid @RequestBody AuthLoginRequest request,
            HttpServletResponse response) {

        Map<String, Object> session =
                authService.logIn(request.email(), request.password());

        registrationService.requireRole(session, UserRole.CUSTOMER);
        startSession(response, session);

        return ResponseEntity.ok(Map.of("message", "Login successful."));
    }

    @PostMapping("/restaurant/login")
    public ResponseEntity<Map<String, String>> restaurantLogin(
            @Valid @RequestBody AuthLoginRequest request,
            HttpServletResponse response) {

        Map<String, Object> session =
                authService.logIn(request.email(), request.password());

        registrationService.requireRole(session, UserRole.RESTAURANT);
        startSession(response, session);

        return ResponseEntity.ok(Map.of("message", "Login successful."));
    }

    @PostMapping("/delivery/login")
    public ResponseEntity<Map<String, String>> deliveryLogin(
            @Valid @RequestBody AuthLoginRequest request,
            HttpServletResponse response) {

        Map<String, Object> session =
                authService.logIn(request.email(), request.password());

        registrationService.requireRole(session, UserRole.DELIVERY_PARTNER);
        startSession(response, session);

        return ResponseEntity.ok(Map.of("message", "Login successful."));
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refresh(
            @CookieValue(
                    name = SupabaseAuthService.REFRESH_COOKIE,
                    required = false
            ) String refreshToken,
            HttpServletResponse response) {

        if (refreshToken == null || refreshToken.isBlank()) {
            throw new AuthException(
                    HttpStatus.UNAUTHORIZED,
                    "You are not logged in."
            );
        }

        Map<String, Object> session = authService.refresh(refreshToken);
        registrationService.requireActiveProfile(session);
        startSession(response, session);

        return ResponseEntity.ok(Map.of("message", "Session refreshed."));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logOut(
            HttpServletResponse response) {

        addCookie(response, SupabaseAuthService.ACCESS_COOKIE, "", 0, "/");
        addCookie(
                response,
                SupabaseAuthService.REFRESH_COOKIE,
                "",
                0,
                AUTH_PATH
        );

        return ResponseEntity.ok(Map.of("message", "Logged out."));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {

        try {
            authService.sendPasswordReset(request.email());
        } catch (AuthException ex) {
            log.warn("Password reset request failed: {}", ex.getMessage());
        }

        return ResponseEntity.ok(Map.of(
                "message",
                "If an account matches that email, password-reset instructions will be sent."
        ));
    }


    @PostMapping("/update-password")
    public ResponseEntity<Map<String, String>> updatePassword(
            @RequestHeader(
                    name = HttpHeaders.AUTHORIZATION,
                    required = false
            ) String authorization,
            @CookieValue(
                    name = SupabaseAuthService.ACCESS_COOKIE,
                    required = false
            ) String cookieAccessToken,
            @Valid @RequestBody UpdatePasswordRequest request) {

        String accessToken;

        if (authorization != null && !authorization.isBlank()) {

            if (!authorization.regionMatches(
                    true, 0, "Bearer ", 0, 7)) {
                throw new AuthException(
                        HttpStatus.UNAUTHORIZED,
                        "Invalid authorization header."
                );
            }

            accessToken = authorization.substring(7).trim();

            if (accessToken.isBlank()) {
                throw new AuthException(
                        HttpStatus.UNAUTHORIZED,
                        "Bearer token is missing."
                );
            }

        } else {

            accessToken = cookieAccessToken;
        }

        if (accessToken == null || accessToken.isBlank()) {
            throw new AuthException(
                    HttpStatus.UNAUTHORIZED,
                    "Your password-reset session is missing or expired. Please verify your recovery code again."
            );
        }

        authService.updatePassword(
                accessToken,
                request.newPassword()
        );

        return ResponseEntity.ok(
                Map.of("message", "Password updated successfully.")
        );
    }



    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @CookieValue(
                    name = SupabaseAuthService.ACCESS_COOKIE,
                    required = false
            ) String accessToken,
            @Valid @RequestBody UpdatePasswordRequest request) {

        if (accessToken == null || accessToken.isBlank()) {
            throw new AuthException(
                    HttpStatus.UNAUTHORIZED,
                    "You must be logged in to change your password."
            );
        }

        authService.updatePassword(accessToken, request.newPassword());

        return ResponseEntity.ok(
                Map.of("message", "Password changed successfully.")
        );
    }

    @GetMapping("/google")
    public void loginWithGoogle(HttpServletResponse response)
            throws IOException {
        startSocialLogin("google", response);
    }

    @GetMapping("/facebook")
    public void loginWithFacebook(HttpServletResponse response)
            throws IOException {
        startSocialLogin("facebook", response);
    }

    @PostMapping("/verify-recovery-otp")
    public ResponseEntity<Map<String, Object>> verifyRecoveryOtp(
            @Valid @RequestBody VerifyOtpRequest request) {

        Map<String, Object> session = authService.verifyRecoveryOtp(
                request.email(),
                request.token()
        );

        Object tokenValue = session.get("access_token");

        if (tokenValue == null || String.valueOf(tokenValue).isBlank()) {
            throw new AuthException(
                    HttpStatus.BAD_GATEWAY,
                    "Recovery verification did not return an access token. Please request a new code."
            );
        }

        Object expiresIn = session.get("expires_in");

        return ResponseEntity.ok(Map.of(
                "message", "Recovery code verified successfully.",
                "access_token", String.valueOf(tokenValue),
                "token_type", "bearer",
                "expires_in", expiresIn instanceof Number ? expiresIn : 3600
        ));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, String>> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request,
            HttpServletResponse response) {

        Map<String, Object> session = authService.verifySignupOtp(
                request.email(),
                request.token()
        );

        registrationService.completeVerifiedRegistration(session);
        startSession(response, session);

        return ResponseEntity.ok(Map.of(
                "message",
                "Email verified successfully. You are now logged in."
        ));
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<Map<String, String>> resendOtp(
            @Valid @RequestBody ResendOtpRequest request) {

        authService.resendSignupOtp(request.email());

        return ResponseEntity.ok(Map.of(
                "message",
                "If signup verification is available, a new code has been requested."
        ));
    }

    @GetMapping("/callback")
    public void socialCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String error,
            @RequestParam(
                    name = "error_description",
                    required = false
            ) String errorDescription,
            @CookieValue(
                    name = SupabaseAuthService.VERIFIER_COOKIE,
                    required = false
            ) String verifier,
            HttpServletResponse response) throws IOException {

        addCookie(
                response,
                SupabaseAuthService.VERIFIER_COOKIE,
                "",
                0,
                AUTH_PATH
        );

        if (error != null) {
            redirectToLoginWithError(
                    response,
                    errorDescription != null
                            ? errorDescription
                            : "Social login was cancelled."
            );
            return;
        }

        if (code == null || verifier == null) {
            redirectToLoginWithError(
                    response,
                    "Social login expired. Please try again."
            );
            return;
        }

        try {
            Map<String, Object> session =
                    authService.exchangeCode(code, verifier);

            authService.createProfileIfMissing(session);
            startSession(response, session);
            response.sendRedirect(frontendUrl + "/");
        } catch (AuthException ex) {
            redirectToLoginWithError(response, ex.getMessage());
        }
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatusException(
            ResponseStatusException ex) {

        String message = ex.getReason() != null
                ? ex.getReason()
                : "Request could not be completed.";

        return ResponseEntity.status(ex.getStatusCode())
                .body(Map.of("message", message));
    }

    private void startSocialLogin(
            String provider,
            HttpServletResponse response) throws IOException {

        String verifier = authService.generateCodeVerifier();
        String challenge = authService.toCodeChallenge(verifier);

        addCookie(
                response,
                SupabaseAuthService.VERIFIER_COOKIE,
                verifier,
                VERIFIER_COOKIE_SECONDS,
                AUTH_PATH
        );

        response.sendRedirect(
                authService.buildSocialLoginUrl(provider, challenge)
        );
    }

    private void startSession(
            HttpServletResponse response,
            Map<String, Object> session) {

        String accessToken = String.valueOf(session.get("access_token"));
        String refreshToken = String.valueOf(session.get("refresh_token"));

        long expiresIn = session.get("expires_in") instanceof Number number
                ? number.longValue()
                : 3600;

        addCookie(
                response,
                SupabaseAuthService.ACCESS_COOKIE,
                accessToken,
                expiresIn,
                "/"
        );

        addCookie(
                response,
                SupabaseAuthService.REFRESH_COOKIE,
                refreshToken,
                REFRESH_COOKIE_SECONDS,
                AUTH_PATH
        );
    }

    private void addCookie(
            HttpServletResponse response,
            String name,
            String value,
            long maxAgeSeconds,
            String path) {

        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path(path)
                .maxAge(maxAgeSeconds)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void redirectToLoginWithError(
            HttpServletResponse response,
            String message) throws IOException {

        response.sendRedirect(
                frontendUrl + "/login?error="
                        + URLEncoder.encode(
                        message,
                        StandardCharsets.UTF_8
                )
        );
    }

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<Map<String, String>> handleAuthException(
            AuthException ex) {

        return ResponseEntity.status(ex.getStatus())
                .body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(
            MethodArgumentNotValidException ex) {

        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(fieldError -> fieldError.getDefaultMessage())
                .orElse("Invalid request.");

        return ResponseEntity.badRequest()
                .body(Map.of("message", message));
    }
}