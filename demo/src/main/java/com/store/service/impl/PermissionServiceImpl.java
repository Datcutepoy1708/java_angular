package com.store.service.impl;

import com.store.dto.role.PermissionGroupResponse;
import com.store.dto.role.PermissionResponse;
import com.store.entity.user.Permission;
import com.store.repository.PermissionRepository;
import com.store.service.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepository permissionRepository;

    @Override
    @Cacheable(cacheNames = "permissions", key = "'allGrouped'")
    public List<PermissionGroupResponse> getAllGroupedPermissions() {
        List<Permission> permissions = permissionRepository.findAllByOrderByPermissionCodeAsc();

        Map<String, List<PermissionResponse>> groupMap = new LinkedHashMap<>();
        groupMap.put("PRODUCT", new ArrayList<>());
        groupMap.put("INVENTORY", new ArrayList<>());
        groupMap.put("ORDER", new ArrayList<>());
        groupMap.put("CHAT", new ArrayList<>());
        groupMap.put("MARKETING", new ArrayList<>());
        groupMap.put("USER", new ArrayList<>());
        groupMap.put("SYSTEM", new ArrayList<>());
        groupMap.put("OTHER", new ArrayList<>());

        for (Permission p : permissions) {
            String group = determineGroup(p.getPermissionCode());
            PermissionResponse resp = PermissionResponse.builder()
                    .permissionId(p.getPermissionId())
                    .permissionCode(p.getPermissionCode())
                    .description(p.getDescription())
                    .moduleGroup(group)
                    .build();
            groupMap.computeIfAbsent(group, k -> new ArrayList<>()).add(resp);
        }

        List<PermissionGroupResponse> result = new ArrayList<>();
        addIfNotEmpty(result, "USER", "Quản Trị, Nhân Sự & Khách Hàng", groupMap.get("USER"));
        addIfNotEmpty(result, "PRODUCT", "Quản Lý Sản Phẩm & Danh Mục", groupMap.get("PRODUCT"));
        addIfNotEmpty(result, "INVENTORY", "Quản Lý Kho Hàng & Nhà Cung Cấp", groupMap.get("INVENTORY"));
        addIfNotEmpty(result, "ORDER", "Quản Lý Đơn Hàng & Đánh Giá", groupMap.get("ORDER"));
        addIfNotEmpty(result, "CHAT", "Chăm Sóc Khách Hàng & Live Chat", groupMap.get("CHAT"));
        addIfNotEmpty(result, "MARKETING", "Khuyến Mãi, Banner & Tin Tức", groupMap.get("MARKETING"));
        addIfNotEmpty(result, "SYSTEM", "Báo Cáo & Cài Đặt Hệ Thống", groupMap.get("SYSTEM"));
        addIfNotEmpty(result, "OTHER", "Quyền Khác", groupMap.get("OTHER"));

        return result;
    }

    @Override
    public List<PermissionResponse> getAllPermissions() {
        return permissionRepository.findAllByOrderByPermissionCodeAsc().stream()
                .map(p -> PermissionResponse.builder()
                        .permissionId(p.getPermissionId())
                        .permissionCode(p.getPermissionCode())
                        .description(p.getDescription())
                        .moduleGroup(determineGroup(p.getPermissionCode()))
                        .build())
                .toList();
    }

    private void addIfNotEmpty(List<PermissionGroupResponse> list, String code, String name, List<PermissionResponse> perms) {
        if (perms != null && !perms.isEmpty()) {
            list.add(PermissionGroupResponse.builder()
                    .groupCode(code)
                    .groupName(name)
                    .permissions(perms)
                    .build());
        }
    }

    private String determineGroup(String code) {
        if (code == null) return "OTHER";
        String upper = code.toUpperCase();
        if (upper.startsWith("USER_") || upper.startsWith("CUSTOMER_") || upper.startsWith("STAFF_") || upper.startsWith("ROLE_")) {
            return "USER";
        }
        if (upper.startsWith("PRODUCT_") || upper.startsWith("CATEGORY_") || upper.startsWith("BRAND_") || upper.startsWith("ATTRIBUTE_")) {
            return "PRODUCT";
        }
        if (upper.startsWith("INVENTORY_") || upper.startsWith("SUPPLIER_") || upper.startsWith("WAREHOUSE_")) {
            return "INVENTORY";
        }
        if (upper.startsWith("ORDER_") || upper.startsWith("REVIEW_")) {
            return "ORDER";
        }
        if (upper.startsWith("CHAT_") || upper.startsWith("BOT_")) {
            return "CHAT";
        }
        if (upper.startsWith("DISCOUNT_") || upper.startsWith("BANNER_") || upper.startsWith("NEWS_")) {
            return "MARKETING";
        }
        if (upper.startsWith("STATISTIC") || upper.startsWith("SETTING_") || upper.startsWith("SYSTEM_")) {
            return "SYSTEM";
        }
        return "OTHER";
    }
}
