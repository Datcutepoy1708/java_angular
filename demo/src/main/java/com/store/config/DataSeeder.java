package com.store.config;

import com.store.entity.user.Permission;
import com.store.entity.user.Role;
import com.store.entity.user.User;
import com.store.entity.user.UserStatus;
import com.store.repository.PermissionRepository;
import com.store.repository.RoleRepository;
import com.store.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        seedPermissionsAndRoles();
        seedDefaultUsers();
    }

    private void seedPermissionsAndRoles() {
        List<String> permissionCodes = List.of(
                "PRODUCT_CREATE", "PRODUCT_UPDATE", "PRODUCT_DELETE", "PRODUCT_VIEW",
                "CATEGORY_MANAGE", "BRAND_MANAGE",
                "ORDER_VIEW", "ORDER_MANAGE",
                "USER_VIEW", "USER_MANAGE"
        );

        Set<Permission> allPermissions = new HashSet<>();
        for (String code : permissionCodes) {
            Permission perm = permissionRepository.findByPermissionCode(code)
                    .orElseGet(() -> permissionRepository.save(
                            Permission.builder()
                                    .permissionCode(code)
                                    .description("Permission for " + code)
                                    .build()
                    ));
            allPermissions.add(perm);
        }

        // ROLE_ADMIN (Full permissions)
        Role adminRole = roleRepository.findByRoleName("ROLE_ADMIN")
                .orElseGet(() -> roleRepository.save(
                        Role.builder()
                                .roleName("ROLE_ADMIN")
                                .description("Full administrative access")
                                .permissions(allPermissions)
                                .build()
                ));
        if (adminRole.getPermissions().isEmpty()) {
            adminRole.setPermissions(allPermissions);
            roleRepository.save(adminRole);
        }

        // ROLE_STAFF (Manage products, orders, categories)
        roleRepository.findByRoleName("ROLE_STAFF")
                .orElseGet(() -> roleRepository.save(
                        Role.builder()
                                .roleName("ROLE_STAFF")
                                .description("Staff member access")
                                .permissions(new HashSet<>(allPermissions))
                                .build()
                ));

        // ROLE_CUSTOMER (Standard shopping customer)
        roleRepository.findByRoleName("ROLE_CUSTOMER")
                .orElseGet(() -> roleRepository.save(
                        Role.builder()
                                .roleName("ROLE_CUSTOMER")
                                .description("Standard shopping customer")
                                .build()
                ));
    }

    private void seedDefaultUsers() {
        Role adminRole = roleRepository.findByRoleName("ROLE_ADMIN").orElse(null);
        Role staffRole = roleRepository.findByRoleName("ROLE_STAFF").orElse(null);
        Role customerRole = roleRepository.findByRoleName("ROLE_CUSTOMER").orElse(null);

        // 1. Default Admin
        if (!userRepository.existsByEmail("admin@store.com")) {
            log.info("Seeding default Administrator account: admin@store.com");
            User admin = User.builder()
                    .fullName("System Administrator")
                    .email("admin@store.com")
                    .phone("0988888888")
                    .passwordHash(passwordEncoder.encode("Admin@123456"))
                    .status(UserStatus.ACTIVE)
                    .emailVerified(true)
                    .roles(adminRole != null ? Set.of(adminRole) : Set.of())
                    .build();
            userRepository.save(admin);
        }

        // 2. Default Staff
        if (!userRepository.existsByEmail("staff@store.com")) {
            log.info("Seeding default Staff account: staff@store.com");
            User staff = User.builder()
                    .fullName("Store Staff Member")
                    .email("staff@store.com")
                    .phone("0977777777")
                    .passwordHash(passwordEncoder.encode("Staff@123456"))
                    .status(UserStatus.ACTIVE)
                    .emailVerified(true)
                    .roles(staffRole != null ? Set.of(staffRole) : Set.of())
                    .build();
            userRepository.save(staff);
        }

        // 3. Default Customer
        if (!userRepository.existsByEmail("customer@store.com")) {
            log.info("Seeding default Customer account: customer@store.com");
            User customer = User.builder()
                    .fullName("Nguyễn Văn Khách")
                    .email("customer@store.com")
                    .phone("0966666666")
                    .passwordHash(passwordEncoder.encode("Customer@123456"))
                    .status(UserStatus.ACTIVE)
                    .emailVerified(true)
                    .roles(customerRole != null ? Set.of(customerRole) : Set.of())
                    .build();
            userRepository.save(customer);
        }
    }
}
