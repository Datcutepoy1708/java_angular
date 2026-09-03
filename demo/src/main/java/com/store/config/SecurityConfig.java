package com.store.config;

import com.store.security.CustomAccessDeniedHandler;
import com.store.security.JwtAuthenticationEntryPoint;
import com.store.security.JwtAuthenticationFilter;
import com.store.security.MaintenanceModeFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final MaintenanceModeFilter maintenanceModeFilter;
    private final JwtAuthenticationEntryPoint unauthorizedHandler;
    private final CustomAccessDeniedHandler accessDeniedHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(unauthorizedHandler)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public Read APIs
                        .requestMatchers(HttpMethod.GET, "/api/v1/categories/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/brands/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/products/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/variants/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/attributes/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/banners/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/news/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/settings/public").permitAll()
                        // Public Inventory Stock Read for Product Details (Guest & Member)
                        .requestMatchers(HttpMethod.GET, "/api/v1/inventory/variants/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/inventory/products/**").permitAll()
                        // Orders public endpoints (Guest Checkout & Order Tracking)
                        .requestMatchers(HttpMethod.POST, "/api/v1/orders").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/orders/track").permitAll()
                        // Admin Settings endpoints
                        .requestMatchers("/api/v1/settings", "/api/v1/settings/**").hasRole("ADMIN")
                        // Static uploads public read
                        .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()
                        // Upload API (Authenticated users: Admin, Staff, Customer for avatars/reviews)
                        .requestMatchers(HttpMethod.POST, "/api/v1/upload/**").authenticated()
                        // Auth APIs
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        // OpenAPI / Swagger & System Error Dispatch
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll()
                        // Actuator (health & info public for probes, others restricted to ADMIN)
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        // WebSocket endpoint (SockJS needs to be public)
                        .requestMatchers("/ws-chat/**").permitAll()
                        // Customer Chat APIs (public — guest access)
                        .requestMatchers("/api/v1/chat/init").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/chat/*/messages").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/chat/upload-image").permitAll()
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/chat/*/mark-read").permitAll()
                        // All other endpoints require authentication
                        .anyRequest().authenticated()
                );

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterAfter(maintenanceModeFilter, JwtAuthenticationFilter.class);

        return http.build();
    }
}
