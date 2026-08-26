package com.store.service;

import com.store.dto.audit.AuditLogFilterRequest;
import com.store.dto.audit.AuditLogResponse;
import org.springframework.data.domain.Page;

public interface AuditLogService {

    Page<AuditLogResponse> getAuditLogs(AuditLogFilterRequest request);

    AuditLogResponse getAuditLogById(Long logId);

    byte[] exportAuditLogsToCsv(AuditLogFilterRequest request);
}
