package com.codewithsvns.foodapp.controller;


import com.codewithsvns.foodapp.dto.AuthLoginRequest;
import com.codewithsvns.foodapp.dto.AuthSignupRequest;
import com.codewithsvns.foodapp.dto.ForgotPasswordRequest;
import com.codewithsvns.foodapp.service.SupabaseAuthService;
import com.codewithsvns.foodapp.service.SupabaseAuthService.AuthException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
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

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private static final long REFRESH_COOKIE_SECONDS = 60L * 60 * 24 * 30; // 30 days
    private static final long VERIFIER_COOKIE_SECONDS = 600;               // 10 minutes
    private static final String AUTH_PATH = "/api/auth";

    private final SupabaseAuthService authService;
    private final String frontendUrl;
    private final boolean cookieSecure;
    private final String cookieSameSite;

    public AuthController(
            SupabaseAuthService authService,
            @Value("${app.frontend-url}") String frontendUrl,
            @Value("${app.cookie.secure}") boolean cookieSecure,
            @Value("${app.cookie.same-site}") String cookieSameSite) {

        this.authService = authService;
        this.frontendUrl = frontendUrl.endsWith("/")
                ? frontendUrl.substring(0, frontendUrl.length() - 1)
                : frontendUrl;
        this.cookieSecure = cookieSecure;
        this.cookieSameSite = cookieSameSite;
    }



    @PostMapping("/signup")
    public ResponseEntity<Map<String, String>> signUp(
            @Valid @RequestBody AuthSignupRequest request,
            HttpServletResponse response) {

        Map<String, Object> result = authService.signUp(
                request.name().trim(), request.email(), request.password());


        if (result.get("access_token") == null) {
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "message", "Account created. Please check your email to verify "
                            + "your account, then log in."));
        }

        startSession(response, result);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Account created successfully."));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> logIn(
            @Valid @RequestBody AuthLoginRequest request,
            HttpServletResponse response) {

        Map<String, Object> session = authService.logIn(request.email(), request.password());

        startSession(response, session);
        return ResponseEntity.ok(Map.of("message", "Login successful."));
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refresh(
            @CookieValue(name = SupabaseAuthService.REFRESH_COOKIE, required = false)
            String refreshToken,
            HttpServletResponse response) {

        if (refreshToken == null || refreshToken.isBlank()) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "You are not logged in.");
        }

        Map<String, Object> session = authService.refresh(refreshToken);

        startSession(response, session);
        return ResponseEntity.ok(Map.of("message", "Session refreshed."));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logOut(HttpServletResponse response) {
        addCookie(response, SupabaseAuthService.ACCESS_COOKIE, "", 0, "/");
        addCookie(response, SupabaseAuthService.REFRESH_COOKIE, "", 0, AUTH_PATH);
        return ResponseEntity.ok(Map.of("message", "Logged out."));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request,
            HttpServletResponse response) {

        String verifier = authService.generateCodeVerifier();
        String challenge = authService.toCodeChallenge(verifier);


        addCookie(response, SupabaseAuthService.VERIFIER_COOKIE, verifier, 3600, AUTH_PATH);

        try {
            authService.sendPasswordReset(request.email(), challenge);
        } catch (AuthException ex) {
            log.warn("Password reset request failed: {}", ex.getMessage());
        }

        return ResponseEntity.ok(Map.of("message",
                "If an account matches that email, password-reset instructions will be sent."));
    }


    @GetMapping("/recovery-callback")
    public void recoveryCallback(
            @RequestParam(required = false) String code,
            @CookieValue(name = SupabaseAuthService.VERIFIER_COOKIE, required = false)
            String verifier,
            HttpServletResponse response) throws IOException {

        addCookie(response, SupabaseAuthService.VERIFIER_COOKIE, "", 0, AUTH_PATH);

        if (code == null || verifier == null) {
            redirectToLoginWithError(response,
                    "Reset link expired or opened in a different browser. Please request a new one.");
            return;
        }

        try {
            Map<String, Object> session = authService.exchangeCode(code, verifier);
            startSession(response, session);
            response.sendRedirect(frontendUrl + "/update-password");
        } catch (AuthException ex) {
            redirectToLoginWithError(response, ex.getMessage());
        }
    }

    @GetMapping("/google")
    public void loginWithGoogle(HttpServletResponse response) throws IOException {
        startSocialLogin("google", response);
    }

    @GetMapping("/facebook")
    public void loginWithFacebook(HttpServletResponse response) throws IOException {
        startSocialLogin("facebook", response);
    }


    @GetMapping("/callback")
    public void socialCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String error,
            @RequestParam(name = "error_description", required = false) String errorDescription,
            @CookieValue(name = SupabaseAuthService.VERIFIER_COOKIE, required = false)
            String verifier,
            HttpServletResponse response) throws IOException {


        addCookie(response, SupabaseAuthService.VERIFIER_COOKIE, "", 0, AUTH_PATH);

        if (error != null) {
            redirectToLoginWithError(response,
                    errorDescription != null ? errorDescription : "Social login was cancelled.");
            return;
        }
        if (code == null || verifier == null) {
            redirectToLoginWithError(response, "Social login expired. Please try again.");
            return;
        }

        try {
            Map<String, Object> session = authService.exchangeCode(code, verifier);
            startSession(response, session);
            response.sendRedirect(frontendUrl + "/");
        } catch (AuthException ex) {
            redirectToLoginWithError(response, ex.getMessage());
        }
    }



    private void startSocialLogin(String provider, HttpServletResponse response)
            throws IOException {

        String verifier = authService.generateCodeVerifier();
        String challenge = authService.toCodeChallenge(verifier);

        addCookie(response, SupabaseAuthService.VERIFIER_COOKIE, verifier,
                VERIFIER_COOKIE_SECONDS, AUTH_PATH);

        response.sendRedirect(authService.buildSocialLoginUrl(provider, challenge));
    }


    private void startSession(HttpServletResponse response, Map<String, Object> session) {
        authService.createProfileIfMissing(session);

        String accessToken = String.valueOf(session.get("access_token"));
        String refreshToken = String.valueOf(session.get("refresh_token"));
        long expiresIn = session.get("expires_in") instanceof Number number
                ? number.longValue()
                : 3600;

        addCookie(response, SupabaseAuthService.ACCESS_COOKIE, accessToken, expiresIn, "/");
        addCookie(response, SupabaseAuthService.REFRESH_COOKIE, refreshToken,
                REFRESH_COOKIE_SECONDS, AUTH_PATH);
    }

    private void addCookie(HttpServletResponse response, String name, String value,
                           long maxAgeSeconds, String path) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path(path)
                .maxAge(maxAgeSeconds)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void redirectToLoginWithError(HttpServletResponse response, String message)
            throws IOException {
        response.sendRedirect(frontendUrl + "/login?error="
                + URLEncoder.encode(message, StandardCharsets.UTF_8));
    }



    @ExceptionHandler(AuthException.class)
    public ResponseEntity<Map<String, String>> handleAuthException(AuthException ex) {
        return ResponseEntity.status(ex.getStatus()).body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> fieldError.getDefaultMessage())
                .orElse("Invalid request.");
        return ResponseEntity.badRequest().body(Map.of("message", message));
    }
}
