package com.store.service.impl;

import com.store.dto.audit.AuditLogFilterRequest;
import com.store.dto.audit.AuditLogResponse;
import com.store.entity.audit.AuditLog;
import com.store.exception.ResourceNotFoundException;
import com.store.repository.AuditLogRepository;
import com.store.service.AuditLogService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditLogServiceImpl implements AuditLogService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AuditLogRepository auditLogRepository;

    @Override
    public Page<AuditLogResponse> getAuditLogs(AuditLogFilterRequest request) {
        log.info("Fetching audit logs with filter: module={}, action={}, user={}, page={}",
                request.getModule(), request.getActionType(), request.getUserId(), request.getPage());

        Sort.Direction direction = "ASC".equalsIgnoreCase(request.getSortDirection()) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String sortBy = request.getSortBy() != null ? request.getSortBy() : "createdAt";
        Pageable pageable = PageRequest.of(Math.max(0, request.getPage()), Math.max(1, request.getSize()), Sort.by(direction, sortBy));

        Specification<AuditLog> spec = buildSpecification(request);
        Page<AuditLog> page = auditLogRepository.findAll(spec, pageable);

        return page.map(this::mapToResponse);
    }

    @Override
    public AuditLogResponse getAuditLogById(Long logId) {
        AuditLog auditLog = auditLogRepository.findById(logId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhật ký kiểm toán với ID: " + logId));
        return mapToResponse(auditLog);
    }

    @Override
    public byte[] exportAuditLogsToCsv(AuditLogFilterRequest request) {
        log.info("Exporting audit logs to CSV");
        Specification<AuditLog> spec = buildSpecification(request);
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        List<AuditLog> logs = auditLogRepository.findAll(spec, sort);

        StringBuilder sb = new StringBuilder();
        // UTF-8 BOM for Excel compatibility
        sb.append('\ufeff');
        sb.append("ID,Thời Gian,Người Thực Hiện,Email,Phân Hệ,Hành Động,Mã Bản Ghi,Mô Tả,Địa Chỉ IP,Trạng Thái\n");

        for (AuditLog l : logs) {
            sb.append(escapeCsv(String.valueOf(l.getLogId()))).append(",")
              .append(escapeCsv(l.getCreatedAt() != null ? l.getCreatedAt().format(DATE_FORMATTER) : "")).append(",")
              .append(escapeCsv(l.getUserId() != null ? String.valueOf(l.getUserId()) : "Hệ thống")).append(",")
              .append(escapeCsv(l.getUserEmail() != null ? l.getUserEmail() : "")).append(",")
              .append(escapeCsv(l.getModule())).append(",")
              .append(escapeCsv(l.getActionType())).append(",")
              .append(escapeCsv(l.getRecordId() != null ? l.getRecordId() : "")).append(",")
              .append(escapeCsv(l.getDescription())).append(",")
              .append(escapeCsv(l.getIpAddress() != null ? l.getIpAddress() : "")).append(",")
              .append(escapeCsv(l.getStatus())).append("\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private Specification<AuditLog> buildSpecification(AuditLogFilterRequest request) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (request.getKeyword() != null && !request.getKeyword().trim().isEmpty()) {
                String pattern = "%" + request.getKeyword().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("description")), pattern),
                        cb.like(cb.lower(root.get("userEmail")), pattern),
                        cb.like(cb.lower(root.get("recordId")), pattern),
                        cb.like(cb.lower(root.get("ipAddress")), pattern)
                ));
            }

            if (request.getModule() != null && !request.getModule().trim().isEmpty()) {
                predicates.add(cb.equal(root.get("module"), request.getModule().trim().toUpperCase()));
            }

            if (request.getActionType() != null && !request.getActionType().trim().isEmpty()) {
                predicates.add(cb.equal(root.get("actionType"), request.getActionType().trim().toUpperCase()));
            }

            if (request.getUserId() != null) {
                predicates.add(cb.equal(root.get("userId"), request.getUserId()));
            }

            if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
                predicates.add(cb.equal(root.get("status"), request.getStatus().trim().toUpperCase()));
            }

            if (request.getFromDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), request.getFromDate().atStartOfDay()));
            }

            if (request.getToDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), request.getToDate().atTime(LocalTime.MAX)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private AuditLogResponse mapToResponse(AuditLog log) {
        return AuditLogResponse.builder()
                .logId(log.getLogId())
                .userId(log.getUserId())
                .userEmail(log.getUserEmail())
                .actionType(log.getActionType())
                .module(log.getModule())
                .recordId(log.getRecordId())
                .description(log.getDescription())
                .oldValue(log.getOldValue())
                .newValue(log.getNewValue())
                .ipAddress(log.getIpAddress())
                .userAgent(log.getUserAgent())
                .status(log.getStatus())
                .createdAt(log.getCreatedAt())
                .build();
    }

    private String escapeCsv(String value) {
        if (value == null) return "\"\"";
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
}
