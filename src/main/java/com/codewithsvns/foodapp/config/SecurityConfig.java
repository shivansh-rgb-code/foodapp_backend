package com.codewithsvns.foodapp.config;

import com.codewithsvns.foodapp.service.SupabaseAuthService;
import jakarta.servlet.http.Cookie;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    private static final String[] PUBLIC_PATHS = {
            "/api/auth/**",
            "/api/webhooks/razorpay",
            "/health",
            "/error",
            "/api/docs/**",
            "/api/openapi/**",
            "/api/swagger-ui/**",
            "/api/customer/ml/health",

            "/api/customer/ml/restaurant-recommendations",

            "/swagger-ui/**",
            "/v3/api-docs/**"
    };

    private static final String[] NO_TOKEN_PREFIXES = {
            "/api/auth/",
            "/api/docs",
            "/api/openapi",
            "/api/swagger-ui",
            "/swagger-ui",
            "/v3/api-docs"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            BearerTokenResolver bearerTokenResolver
    ) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .bearerTokenResolver(bearerTokenResolver)
                        .jwt(Customizer.withDefaults())
                );

        http.cors(Customizer.withDefaults());

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(
                List.of(
                        "http://localhost:5173",
                        "http://localhost:5174",
                        "https://hungry-hour-food-order-and-delivery.vercel.app"
                )
        );

        configuration.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "PATCH",
                        "DELETE",
                        "OPTIONS"
                )
        );

        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    @Bean
    public BearerTokenResolver bearerTokenResolver() {
        DefaultBearerTokenResolver headerResolver =
                new DefaultBearerTokenResolver();

        return request -> {
            String uri = request.getRequestURI();

            for (String prefix : NO_TOKEN_PREFIXES) {
                if (uri.startsWith(prefix)) {
                    return null;
                }
            }

            String tokenFromHeader = headerResolver.resolve(request);

            if (tokenFromHeader != null) {
                return tokenFromHeader;
            }

            if (request.getCookies() != null) {
                for (Cookie cookie : request.getCookies()) {
                    if (SupabaseAuthService.ACCESS_COOKIE.equals(
                            cookie.getName()
                    )) {
                        return cookie.getValue();
                    }
                }
            }

            return null;
        };
    }
}
