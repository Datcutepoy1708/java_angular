package com.store.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.store.dto.response.ApiResponse;
import com.store.service.SettingService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class MaintenanceModeFilter extends OncePerRequestFilter {

    private final SettingService settingService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        if (isWhitelisted(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            if (settingService.isMaintenanceModeActive()) {
                log.warn("Maintenance mode active: Rejecting public request to URI '{}' with HTTP 503", request.getRequestURI());
                response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                response.setHeader(HttpHeaders.RETRY_AFTER, "3600");
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding("UTF-8");

                ApiResponse<Void> apiResponse = ApiResponse.error(
                        "Hệ thống đang tạm ngừng phục vụ để bảo trì & nâng cấp hạ tầng. Vui lòng quay lại sau."
                );

                response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
                return;
            }
        } catch (Exception ex) {
            log.error("Failed to check maintenance mode status, allowing request through fallback", ex);
        }

        filterChain.doFilter(request, response);
    }

    private boolean isWhitelisted(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String method = request.getMethod();

        // 1. Admin endpoints
        if (uri.startsWith("/api/v1/admin") || uri.startsWith("/api/v1/settings")) {
            // Note: GET /api/v1/settings/public is explicitly public so FE can read settings
            return true;
        }

        // 2. Authentication endpoints
        if (uri.startsWith("/api/v1/auth")) {
            return true;
        }

        // 3. Static & System documentation / health
        if (uri.startsWith("/swagger-ui") || uri.startsWith("/v3/api-docs") || uri.startsWith("/actuator") || "/favicon.ico".equals(uri)) {
            return true;
        }

        // 4. Authenticated Admin / Staff users
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            for (GrantedAuthority authority : auth.getAuthorities()) {
                String role = authority.getAuthority();
                if ("ROLE_ADMIN".equals(role) || "ROLE_STAFF".equals(role) || "ADMIN".equals(role) || "STAFF".equals(role)) {
                    return true;
                }
            }
        }

        return false;
    }
}
