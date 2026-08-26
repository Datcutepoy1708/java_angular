package com.store.audit.aspect;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.store.audit.annotation.Auditable;
import com.store.audit.event.AuditLogEvent;
import com.store.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Set;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private static final Set<String> SENSITIVE_FIELDS = Set.of(
            "password", "passwordhash", "password_hash",
            "token", "refreshtoken", "refresh_token", "accesstoken", "access_token",
            "bankaccountnumber", "bank_account_number", "cardnumber", "cvv", "secret"
    );

    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @Around("@annotation(com.store.audit.annotation.Auditable)")
    public Object auditMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Auditable auditable = method.getAnnotation(Auditable.class);

        Long currentUserId = null;
        String currentUserEmail = "anonymous";

        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof CustomUserDetails userDetails) {
                currentUserId = userDetails.getUserId();
                currentUserEmail = userDetails.getEmail();
            } else if (auth != null && auth.getName() != null) {
                currentUserEmail = auth.getName();
            }
        } catch (Exception e) {
            log.warn("Could not extract user principal for audit log: {}", e.getMessage());
        }

        String ipAddress = null;
        String userAgent = null;
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest req = attributes.getRequest();
                ipAddress = getClientIp(req);
                userAgent = req.getHeader("User-Agent");
                if (userAgent != null && userAgent.length() > 250) {
                    userAgent = userAgent.substring(0, 250);
                }
            }
        } catch (Exception e) {
            log.warn("Could not extract HTTP servlet attributes for audit log: {}", e.getMessage());
        }

        String requestPayload = serializeAndMask(joinPoint.getArgs());
        Object result = null;
        String status = "SUCCESS";

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable t) {
            status = "FAILED";
            throw t;
        } finally {
            try {
                String responsePayload = result != null ? serializeAndMask(result) : null;
                String desc = auditable.description();
                if (desc == null || desc.isBlank()) {
                    desc = method.getName() + " on module " + auditable.module();
                }

                AuditLogEvent event = AuditLogEvent.builder()
                        .userId(currentUserId)
                        .userEmail(currentUserEmail)
                        .actionType(auditable.actionType())
                        .module(auditable.module())
                        .description(desc)
                        .oldValue(requestPayload)
                        .newValue(responsePayload)
                        .ipAddress(ipAddress)
                        .userAgent(userAgent)
                        .status(status)
                        .build();

                eventPublisher.publishEvent(event);
            } catch (Exception e) {
                log.error("Error creating and publishing audit log event: {}", e.getMessage(), e);
            }
        }
    }

    private String serializeAndMask(Object obj) {
        if (obj == null) return null;
        try {
            JsonNode node = objectMapper.valueToTree(obj);
            maskSensitiveNode(node);
            String json = objectMapper.writeValueAsString(node);
            return json.length() > 5000 ? json.substring(0, 5000) + "...[truncated]" : json;
        } catch (Exception e) {
            return "[Unable to serialize: " + e.getMessage() + "]";
        }
    }

    private void maskSensitiveNode(JsonNode node) {
        if (node == null) return;
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            Iterator<String> fieldNames = objectNode.fieldNames();
            while (fieldNames.hasNext()) {
                String field = fieldNames.next();
                if (SENSITIVE_FIELDS.contains(field.toLowerCase())) {
                    objectNode.put(field, "******");
                } else {
                    maskSensitiveNode(objectNode.get(field));
                }
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                maskSensitiveNode(child);
            }
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isBlank() || "unknown".equalsIgnoreCase(xfHeader)) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}
