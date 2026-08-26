package com.store.audit.listener;

import com.store.audit.event.AuditLogEvent;
import com.store.entity.audit.AuditLog;
import com.store.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLogEventListener {

    private final AuditLogRepository auditLogRepository;

    @Async
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleAuditLogEvent(AuditLogEvent event) {
        try {
            AuditLog logEntry = AuditLog.builder()
                    .userId(event.getUserId())
                    .userEmail(event.getUserEmail())
                    .actionType(event.getActionType() != null ? event.getActionType() : "UNKNOWN")
                    .module(event.getModule() != null ? event.getModule() : "SYSTEM")
                    .recordId(event.getRecordId())
                    .description(event.getDescription() != null ? event.getDescription() : "")
                    .oldValue(event.getOldValue())
                    .newValue(event.getNewValue())
                    .ipAddress(event.getIpAddress())
                    .userAgent(event.getUserAgent())
                    .status(event.getStatus() != null ? event.getStatus() : "SUCCESS")
                    .build();

            auditLogRepository.save(logEntry);
            log.debug("AuditLog saved successfully: {} - {}", event.getModule(), event.getActionType());
        } catch (Exception e) {
            log.error("Failed to persist audit log entry asynchronously: {}", e.getMessage(), e);
        }
    }
}
