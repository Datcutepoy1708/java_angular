package com.store.controller.admin;

import com.store.dto.audit.AuditLogFilterRequest;
import com.store.dto.audit.AuditLogResponse;
import com.store.dto.response.ApiResponse;
import com.store.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@RequiredArgsConstructor
@Tag(name = "Admin Audit Logs", description = "APIs for querying and exporting system audit logs")
@PreAuthorize("hasRole('ADMIN')")
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    @Operation(summary = "Get paginated audit logs with dynamic filtering")
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> getAuditLogs(
            @ModelAttribute AuditLogFilterRequest request
    ) {
        Page<AuditLogResponse> page = auditLogService.getAuditLogs(request);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách nhật ký kiểm toán thành công", page));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get audit log details by ID")
    public ResponseEntity<ApiResponse<AuditLogResponse>> getAuditLogById(@PathVariable("id") Long logId) {
        AuditLogResponse response = auditLogService.getAuditLogById(logId);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết nhật ký thành công", response));
    }

    @GetMapping("/export")
    @Operation(summary = "Export audit logs matching filter to CSV file")
    public ResponseEntity<byte[]> exportAuditLogsToCsv(@ModelAttribute AuditLogFilterRequest request) {
        byte[] csvData = auditLogService.exportAuditLogsToCsv(request);
        String filename = "audit_logs_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvData);
    }
}
