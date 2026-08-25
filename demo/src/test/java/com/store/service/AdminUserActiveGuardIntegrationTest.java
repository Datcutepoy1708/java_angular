package com.store.service;

import com.store.entity.user.AuthProvider;
import com.store.entity.user.Role;
import com.store.entity.user.User;
import com.store.entity.user.UserStatus;
import com.store.repository.RoleRepository;
import com.store.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "jwt.secret=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970")
@Transactional
class AdminUserActiveGuardIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private Role adminRole;
    private Role staffRole;
    private Role customerRole;

    @BeforeEach
    void setUp() {
        adminRole = roleRepository.findByRoleName("ROLE_ADMIN")
                .orElseGet(() -> roleRepository.save(Role.builder().roleName("ROLE_ADMIN").description("Admin").build()));
        staffRole = roleRepository.findByRoleName("ROLE_STAFF")
                .orElseGet(() -> roleRepository.save(Role.builder().roleName("ROLE_STAFF").description("Staff").build()));
        customerRole = roleRepository.findByRoleName("ROLE_CUSTOMER")
                .orElseGet(() -> roleRepository.save(Role.builder().roleName("ROLE_CUSTOMER").description("Customer").build()));
    }

    @Test
    @DisplayName("Case 6: countActiveAdmins() in real database ONLY counts ROLE_ADMIN with status = ACTIVE")
    void countActiveAdmins_IgnoresInactiveOrBannedAdmins() {
        long baseActiveCount = userRepository.countActiveAdmins();

        long ts = System.currentTimeMillis();

        // 1. Create a BANNED admin -> must NOT increment countActiveAdmins()
        User bannedAdmin = userRepository.save(User.builder()
                .fullName("Banned Admin " + ts)
                .email("banned_" + ts + "@store.com")
                .passwordHash("hashed")
                .status(UserStatus.BANNED)
                .roles(new HashSet<>(Set.of(adminRole)))
                .emailVerified(true)
                .provider(AuthProvider.LOCAL)
                .build());

        userRepository.flush();
        assertThat(userRepository.countActiveAdmins()).isEqualTo(baseActiveCount);

        // 2. Create an INACTIVE admin -> must NOT increment countActiveAdmins()
        User inactiveAdmin = userRepository.save(User.builder()
                .fullName("Inactive Admin " + ts)
                .email("inactive_" + ts + "@store.com")
                .passwordHash("hashed")
                .status(UserStatus.INACTIVE)
                .roles(new HashSet<>(Set.of(adminRole)))
                .emailVerified(true)
                .provider(AuthProvider.LOCAL)
                .build());

        userRepository.flush();
        assertThat(userRepository.countActiveAdmins()).isEqualTo(baseActiveCount);

        // 3. Create an ACTIVE non-admin user (ROLE_CUSTOMER + ROLE_STAFF) -> must NOT increment countActiveAdmins()
        User activeStaff = userRepository.save(User.builder()
                .fullName("Active Staff " + ts)
                .email("staff_" + ts + "@store.com")
                .passwordHash("hashed")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(staffRole, customerRole)))
                .emailVerified(true)
                .provider(AuthProvider.LOCAL)
                .build());

        userRepository.flush();
        assertThat(userRepository.countActiveAdmins()).isEqualTo(baseActiveCount);

        // 4. Create an ACTIVE admin -> MUST increment countActiveAdmins() by exactly 1
        User newActiveAdmin = userRepository.save(User.builder()
                .fullName("New Active Admin " + ts)
                .email("active_admin_" + ts + "@store.com")
                .passwordHash("hashed")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(adminRole)))
                .emailVerified(true)
                .provider(AuthProvider.LOCAL)
                .build());

        userRepository.flush();
        assertThat(userRepository.countActiveAdmins()).isEqualTo(baseActiveCount + 1);

        // 5. Changing newActiveAdmin status to BANNED -> countActiveAdmins() immediately drops back to baseActiveCount
        newActiveAdmin.setStatus(UserStatus.BANNED);
        userRepository.save(newActiveAdmin);
        userRepository.flush();
        assertThat(userRepository.countActiveAdmins()).isEqualTo(baseActiveCount);

        // 6. Changing bannedAdmin status to ACTIVE -> countActiveAdmins() increments by 1
        bannedAdmin.setStatus(UserStatus.ACTIVE);
        userRepository.save(bannedAdmin);
        userRepository.flush();
        assertThat(userRepository.countActiveAdmins()).isEqualTo(baseActiveCount + 1);

        // 7. Removing ROLE_ADMIN from bannedAdmin -> countActiveAdmins() drops back to baseActiveCount
        bannedAdmin.setRoles(new HashSet<>(Set.of(customerRole)));
        userRepository.save(bannedAdmin);
        userRepository.flush();
        assertThat(userRepository.countActiveAdmins()).isEqualTo(baseActiveCount);
    }
}
