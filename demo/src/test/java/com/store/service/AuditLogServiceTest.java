package com.store.service;

import com.store.dto.audit.AuditLogFilterRequest;
import com.store.dto.audit.AuditLogResponse;
import com.store.entity.audit.AuditLog;
import com.store.exception.ResourceNotFoundException;
import com.store.repository.AuditLogRepository;
import com.store.service.impl.AuditLogServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogServiceImpl auditLogService;

    private AuditLog sampleLog;

    @BeforeEach
    void setUp() {
        sampleLog = AuditLog.builder()
                .logId(1L)
                .userId(1L)
                .userEmail("admin@store.com")
                .actionType("UPDATE")
                .module("ROLE")
                .recordId("ROLE_STAFF")
                .description("Cập nhật quyền hạn cho chức vụ Nhân viên")
                .oldValue("{\"roleName\":\"ROLE_STAFF\"}")
                .newValue("{\"roleName\":\"ROLE_STAFF\",\"permissions\":[\"PRODUCT_VIEW\"]}")
                .ipAddress("127.0.0.1")
                .userAgent("Mozilla/5.0")
                .status("SUCCESS")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("getAuditLogs should return paginated audit logs")
    void getAuditLogs_Success() {
        AuditLogFilterRequest request = AuditLogFilterRequest.builder()
                .module("ROLE")
                .actionType("UPDATE")
                .page(0)
                .size(10)
                .build();

        Page<AuditLog> page = new PageImpl<>(List.of(sampleLog));
        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Page<AuditLogResponse> result = auditLogService.getAuditLogs(request);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getUserEmail()).isEqualTo("admin@store.com");
        assertThat(result.getContent().get(0).getModule()).isEqualTo("ROLE");
    }

    @Test
    @DisplayName("getAuditLogById should return details when log exists")
    void getAuditLogById_Success() {
        when(auditLogRepository.findById(1L)).thenReturn(Optional.of(sampleLog));

        AuditLogResponse result = auditLogService.getAuditLogById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getLogId()).isEqualTo(1L);
        assertThat(result.getDescription()).contains("Cập nhật quyền hạn");
    }

    @Test
    @DisplayName("getAuditLogById should throw ResourceNotFoundException when log not found")
    void getAuditLogById_NotFound() {
        when(auditLogRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> auditLogService.getAuditLogById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("exportAuditLogsToCsv should return UTF-8 CSV bytes")
    void exportAuditLogsToCsv_Success() {
        AuditLogFilterRequest request = AuditLogFilterRequest.builder().module("ROLE").build();
        when(auditLogRepository.findAll(any(Specification.class), any(org.springframework.data.domain.Sort.class)))
                .thenReturn(List.of(sampleLog));

        byte[] csvData = auditLogService.exportAuditLogsToCsv(request);

        assertThat(csvData).isNotEmpty();
        String csvString = new String(csvData, StandardCharsets.UTF_8);
        assertThat(csvString).contains("ID,Thời Gian,Người Thực Hiện,Email,Phân Hệ");
        assertThat(csvString).contains("admin@store.com");
        assertThat(csvString).contains("ROLE");
    }
}
