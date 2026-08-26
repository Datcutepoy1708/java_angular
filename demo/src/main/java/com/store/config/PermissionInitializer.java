package com.store.config;

import com.store.entity.user.Permission;
import com.store.entity.user.Role;
import com.store.repository.PermissionRepository;
import com.store.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.cache.CacheManager;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class PermissionInitializer implements CommandLineRunner {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final CacheManager cacheManager;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Checking and initializing full CRUD permissions for all modules...");

        Map<String, String> fullCrudPermissions = new LinkedHashMap<>();

        // 1. Quản trị, Nhân Sự & Khách Hàng (USER)
        fullCrudPermissions.put("ROLE_VIEW", "Xem danh sách chức vụ & vai trò");
        fullCrudPermissions.put("ROLE_CREATE", "Tạo chức vụ mới");
        fullCrudPermissions.put("ROLE_UPDATE", "Cập nhật chức vụ & phân quyền");
        fullCrudPermissions.put("ROLE_DELETE", "Xóa chức vụ");
        fullCrudPermissions.put("ROLE_MANAGE", "Toàn quyền quản lý chức vụ");

        fullCrudPermissions.put("STAFF_VIEW", "Xem danh sách nhân viên");
        fullCrudPermissions.put("STAFF_CREATE", "Thêm nhân viên mới");
        fullCrudPermissions.put("STAFF_UPDATE", "Cập nhật thông tin nhân sự");
        fullCrudPermissions.put("STAFF_DELETE", "Xóa tài khoản nhân viên");
        fullCrudPermissions.put("STAFF_RESET_PWD", "Đặt lại mật khẩu nhân viên");
        fullCrudPermissions.put("STAFF_MANAGE", "Toàn quyền quản lý nhân sự");

        fullCrudPermissions.put("CUSTOMER_VIEW", "Xem danh sách khách hàng & chi tiêu");
        fullCrudPermissions.put("CUSTOMER_UPDATE", "Cập nhật hồ sơ khách hàng");
        fullCrudPermissions.put("CUSTOMER_STATUS", "Khóa / Mở khóa tài khoản khách hàng");
        fullCrudPermissions.put("CUSTOMER_RESET_PWD", "Đặt lại mật khẩu khách hàng");

        fullCrudPermissions.put("USER_VIEW", "Xem danh sách người dùng hệ thống");
        fullCrudPermissions.put("USER_MANAGE", "Quản lý người dùng toàn hệ thống");

        // 2. Sản Phẩm & Danh Mục (PRODUCT)
        fullCrudPermissions.put("PRODUCT_VIEW", "Xem danh sách và chi tiết sản phẩm");
        fullCrudPermissions.put("PRODUCT_CREATE", "Thêm mới sản phẩm");
        fullCrudPermissions.put("PRODUCT_UPDATE", "Cập nhật sản phẩm");
        fullCrudPermissions.put("PRODUCT_DELETE", "Xóa sản phẩm");

        fullCrudPermissions.put("CATEGORY_VIEW", "Xem danh mục sản phẩm");
        fullCrudPermissions.put("CATEGORY_CREATE", "Thêm mới danh mục");
        fullCrudPermissions.put("CATEGORY_UPDATE", "Cập nhật danh mục");
        fullCrudPermissions.put("CATEGORY_DELETE", "Xóa danh mục");
        fullCrudPermissions.put("CATEGORY_MANAGE", "Quản lý danh mục sản phẩm");

        fullCrudPermissions.put("BRAND_VIEW", "Xem danh sách thương hiệu");
        fullCrudPermissions.put("BRAND_CREATE", "Thêm mới thương hiệu");
        fullCrudPermissions.put("BRAND_UPDATE", "Cập nhật thương hiệu");
        fullCrudPermissions.put("BRAND_DELETE", "Xóa thương hiệu");
        fullCrudPermissions.put("BRAND_MANAGE", "Quản lý thương hiệu sản phẩm");

        fullCrudPermissions.put("ATTRIBUTE_VIEW", "Xem danh mục thuộc tính (EAV)");
        fullCrudPermissions.put("ATTRIBUTE_MANAGE", "Quản lý thuộc tính và thông số kỹ thuật");

        // 3. Kho Hàng & Nhà Cung Cấp (INVENTORY)
        fullCrudPermissions.put("INVENTORY_VIEW", "Xem tồn kho và lịch sử xuất nhập");
        fullCrudPermissions.put("INVENTORY_IMPORT", "Nhập hàng vào kho (phiếu nhập)");
        fullCrudPermissions.put("INVENTORY_TRANSFER", "Điều chuyển hàng giữa các kho");
        fullCrudPermissions.put("INVENTORY_MANAGE", "Quản lý tồn kho và kho bãi");

        fullCrudPermissions.put("WAREHOUSE_VIEW", "Xem danh sách kho hàng");
        fullCrudPermissions.put("WAREHOUSE_MANAGE", "Quản lý danh sách kho hàng");

        fullCrudPermissions.put("SUPPLIER_VIEW", "Xem danh sách nhà cung cấp");
        fullCrudPermissions.put("SUPPLIER_CREATE", "Thêm mới nhà cung cấp");
        fullCrudPermissions.put("SUPPLIER_UPDATE", "Cập nhật nhà cung cấp");
        fullCrudPermissions.put("SUPPLIER_DELETE", "Xóa nhà cung cấp");
        fullCrudPermissions.put("SUPPLIER_MANAGE", "Quản lý nhà cung cấp");

        // 4. Đơn Hàng & Đánh Giá (ORDER)
        fullCrudPermissions.put("ORDER_VIEW", "Xem danh sách và chi tiết đơn hàng");
        fullCrudPermissions.put("ORDER_UPDATE_STATUS", "Cập nhật trạng thái đơn hàng");
        fullCrudPermissions.put("ORDER_CANCEL", "Hủy đơn hàng");
        fullCrudPermissions.put("ORDER_MANAGE", "Quản lý toàn diện đơn hàng");

        fullCrudPermissions.put("REVIEW_VIEW", "Xem danh sách đánh giá sản phẩm");
        fullCrudPermissions.put("REVIEW_REPLY", "Phản hồi đánh giá khách hàng");
        fullCrudPermissions.put("REVIEW_DELETE", "Xóa / ẩn đánh giá vi phạm");

        // 5. Khuyến Mãi & Tiếp Thị (MARKETING)
        fullCrudPermissions.put("DISCOUNT_VIEW", "Xem danh sách mã giảm giá");
        fullCrudPermissions.put("DISCOUNT_CREATE", "Tạo mới mã giảm giá");
        fullCrudPermissions.put("DISCOUNT_UPDATE", "Cập nhật mã giảm giá");
        fullCrudPermissions.put("DISCOUNT_DELETE", "Xóa mã giảm giá");
        fullCrudPermissions.put("DISCOUNT_MANAGE", "Quản lý mã giảm giá");

        fullCrudPermissions.put("BANNER_VIEW", "Xem danh sách banner quảng cáo");
        fullCrudPermissions.put("BANNER_CREATE", "Thêm mới banner quảng cáo");
        fullCrudPermissions.put("BANNER_UPDATE", "Cập nhật banner quảng cáo");
        fullCrudPermissions.put("BANNER_DELETE", "Xóa banner quảng cáo");
        fullCrudPermissions.put("BANNER_MANAGE", "Quản lý banner quảng cáo");

        fullCrudPermissions.put("NEWS_VIEW", "Xem danh sách bài viết tin tức");
        fullCrudPermissions.put("NEWS_CREATE", "Soạn thảo bài viết mới");
        fullCrudPermissions.put("NEWS_UPDATE", "Chỉnh sửa bài viết tin tức");
        fullCrudPermissions.put("NEWS_DELETE", "Xóa bài viết tin tức");
        fullCrudPermissions.put("NEWS_MANAGE", "Quản lý tin tức và bài viết");

        // 6. Báo Cáo & Cài Đặt Hệ Thống (SYSTEM)
        fullCrudPermissions.put("STATISTIC_VIEW", "Xem báo cáo doanh thu & thống kê");
        fullCrudPermissions.put("STATISTICS_VIEW", "Xem biểu đồ phân tích kinh doanh");
        fullCrudPermissions.put("SETTING_VIEW", "Xem thông tin cấu hình hệ thống");
        fullCrudPermissions.put("SETTING_UPDATE", "Cập nhật cài đặt website & thanh toán");
        fullCrudPermissions.put("SETTING_MANAGE", "Quản trị toàn diện cấu hình hệ thống");

        boolean hasNewPermissions = false;
        List<Permission> allPermissions = new ArrayList<>();

        for (Map.Entry<String, String> entry : fullCrudPermissions.entrySet()) {
            String code = entry.getKey();
            String desc = entry.getValue();

            Optional<Permission> existing = permissionRepository.findByPermissionCode(code);
            if (existing.isEmpty()) {
                Permission newPerm = Permission.builder()
                        .permissionCode(code)
                        .description(desc)
                        .build();
                permissionRepository.save(newPerm);
                allPermissions.add(newPerm);
                hasNewPermissions = true;
                log.info("Seeded new CRUD permission: {} ({})", code, desc);
            } else {
                allPermissions.add(existing.get());
            }
        }

        // Ensure ROLE_ADMIN has all permissions
        Optional<Role> adminRoleOpt = roleRepository.findByRoleName("ROLE_ADMIN");
        if (adminRoleOpt.isPresent()) {
            Role adminRole = adminRoleOpt.get();
            Set<Permission> currentPerms = adminRole.getPermissions();
            if (currentPerms == null) {
                currentPerms = new HashSet<>();
            }
            int initialCount = currentPerms.size();
            currentPerms.addAll(allPermissions);
            if (currentPerms.size() > initialCount) {
                adminRole.setPermissions(currentPerms);
                roleRepository.save(adminRole);
                log.info("Assigned {} permissions to ROLE_ADMIN", currentPerms.size());
            }
        }

        if (hasNewPermissions) {
            var cache = cacheManager.getCache("permissions");
            if (cache != null) {
                cache.clear();
            }
            var rolesCache = cacheManager.getCache("roles");
            if (rolesCache != null) {
                rolesCache.clear();
            }
        }
    }
}
