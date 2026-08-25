package com.store.service;

import com.store.dto.response.PageResponse;
import com.store.dto.user.admin.AdminUserCreateRequest;
import com.store.dto.user.admin.AdminUserPasswordResetRequest;
import com.store.dto.user.admin.AdminUserResponse;
import com.store.dto.user.admin.AdminUserStatusRequest;
import com.store.dto.user.admin.AdminUserUpdateRequest;
import com.store.dto.user.admin.RoleResponse;
import com.store.entity.user.AuthProvider;
import com.store.entity.user.Gender;
import com.store.entity.user.Role;
import com.store.entity.user.User;
import com.store.entity.user.UserStatus;
import com.store.exception.BadRequestException;
import com.store.exception.DuplicateResourceException;
import com.store.exception.ResourceNotFoundException;
import com.store.repository.OrderRepository;
import com.store.repository.RoleRepository;
import com.store.repository.UserRepository;
import com.store.service.impl.AdminUserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminUserServiceImpl adminUserService;

    private Role adminRole;
    private Role staffRole;
    private Role customerRole;
    private User testAdmin;
    private User testCustomer;

    @BeforeEach
    void setUp() {
        adminRole = Role.builder().roleId(1).roleName("ROLE_ADMIN").description("Quản trị hệ thống").build();
        staffRole = Role.builder().roleId(2).roleName("ROLE_STAFF").description("Nhân viên bán hàng").build();
        customerRole = Role.builder().roleId(3).roleName("ROLE_CUSTOMER").description("Khách hàng").build();

        testAdmin = User.builder()
                .userId(1L)
                .fullName("Super Admin")
                .email("admin@complexus.com")
                .phone("0901112233")
                .passwordHash("hashed_admin_pwd")
                .gender(Gender.MALE)
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .provider(AuthProvider.LOCAL)
                .roles(new HashSet<>(Set.of(adminRole)))
                .createdAt(LocalDateTime.now())
                .build();

        testCustomer = User.builder()
                .userId(2L)
                .fullName("Nguyễn Văn Khách")
                .email("customer@gmail.com")
                .phone("0909998877")
                .passwordHash("hashed_customer_pwd")
                .gender(Gender.FEMALE)
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .provider(AuthProvider.LOCAL)
                .roles(new HashSet<>(Set.of(customerRole)))
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("Read Operations")
    class ReadTests {

        @Test
        @DisplayName("getUsersPaginated should return page with order stats")
        void getUsersPaginated_Success() {
            Page<User> page = new PageImpl<>(List.of(testCustomer));
            when(userRepository.findAllFiltered(eq("khach"), eq("ROLE_CUSTOMER"), eq(UserStatus.ACTIVE), any(Pageable.class)))
                    .thenReturn(page);
            when(orderRepository.countByUserUserId(2L)).thenReturn(3L);
            when(orderRepository.sumTotalSpendByUserId(2L)).thenReturn(BigDecimal.valueOf(15000000));

            PageResponse<AdminUserResponse> result = adminUserService.getUsersPaginated(
                    0, 10, "khach", "ROLE_CUSTOMER", "active", "createdAt", "desc"
            );

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getFullName()).isEqualTo("Nguyễn Văn Khách");
            assertThat(result.getContent().get(0).getTotalOrders()).isEqualTo(3L);
            assertThat(result.getContent().get(0).getTotalSpend()).isEqualByComparingTo(BigDecimal.valueOf(15000000));
        }

        @Test
        @DisplayName("getUserById should return user when found")
        void getUserById_Success() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testAdmin));
            when(orderRepository.countByUserUserId(1L)).thenReturn(0L);
            when(orderRepository.sumTotalSpendByUserId(1L)).thenReturn(BigDecimal.ZERO);

            AdminUserResponse response = adminUserService.getUserById(1L);

            assertThat(response).isNotNull();
            assertThat(response.getUserId()).isEqualTo(1L);
            assertThat(response.getRoles()).contains("ROLE_ADMIN");
        }

        @Test
        @DisplayName("getUserById should throw ResourceNotFoundException when not found")
        void getUserById_NotFound() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adminUserService.getUserById(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("999");
        }

        @Test
        @DisplayName("getAllRoles should return all roles")
        void getAllRoles_Success() {
            when(roleRepository.findAllByOrderByRoleIdAsc()).thenReturn(List.of(adminRole, staffRole, customerRole));

            List<RoleResponse> result = adminUserService.getAllRoles();

            assertThat(result).hasSize(3);
            assertThat(result.get(0).getRoleName()).isEqualTo("ROLE_ADMIN");
        }
    }

    @Nested
    @DisplayName("Create User Operations")
    class CreateTests {

        @Test
        @DisplayName("createUser should create user with encoded password and assigned roles")
        void createUser_Success() {
            AdminUserCreateRequest request = AdminUserCreateRequest.builder()
                    .fullName("Nhân Viên Mới")
                    .email("staff@complexus.com")
                    .phone("0988776655")
                    .password("Staff@123456")
                    .gender("male")
                    .birthDate(LocalDate.of(1995, 5, 20))
                    .roles(Set.of("ROLE_STAFF"))
                    .status("active")
                    .build();

            when(userRepository.existsByEmail("staff@complexus.com")).thenReturn(false);
            when(userRepository.existsByPhone("0988776655")).thenReturn(false);
            when(roleRepository.findByRoleName("ROLE_STAFF")).thenReturn(Optional.of(staffRole));
            when(passwordEncoder.encode("Staff@123456")).thenReturn("hashed_new_pwd");
            when(userRepository.save(any(User.class))).thenAnswer(i -> {
                User u = i.getArgument(0);
                u.setUserId(3L);
                return u;
            });

            AdminUserResponse response = adminUserService.createUser(request);

            assertThat(response).isNotNull();
            assertThat(response.getUserId()).isEqualTo(3L);
            assertThat(response.getEmail()).isEqualTo("staff@complexus.com");
            assertThat(response.getRoles()).contains("ROLE_STAFF");
            verify(passwordEncoder).encode("Staff@123456");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("createUser should throw DuplicateResourceException when email already exists")
        void createUser_DuplicateEmail() {
            AdminUserCreateRequest request = AdminUserCreateRequest.builder()
                    .email("admin@complexus.com")
                    .build();

            when(userRepository.existsByEmail("admin@complexus.com")).thenReturn(true);

            assertThatThrownBy(() -> adminUserService.createUser(request))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("Email");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("createUser should throw DuplicateResourceException when phone already exists")
        void createUser_DuplicatePhone() {
            AdminUserCreateRequest request = AdminUserCreateRequest.builder()
                    .email("unique@complexus.com")
                    .phone("0901112233")
                    .build();

            when(userRepository.existsByEmail("unique@complexus.com")).thenReturn(false);
            when(userRepository.existsByPhone("0901112233")).thenReturn(true);

            assertThatThrownBy(() -> adminUserService.createUser(request))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("Số điện thoại");

            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Update & Active Admin Guard Operations")
    class UpdateTests {

        @Test
        @DisplayName("updateUser should succeed for customer update")
        void updateUser_Customer_Success() {
            AdminUserUpdateRequest request = AdminUserUpdateRequest.builder()
                    .fullName("Nguyễn Văn Khách VIP")
                    .phone("0909998877")
                    .gender("male")
                    .roles(Set.of("ROLE_CUSTOMER"))
                    .status("active")
                    .build();

            when(userRepository.findById(2L)).thenReturn(Optional.of(testCustomer));
            when(userRepository.existsByPhoneAndUserIdNot("0909998877", 2L)).thenReturn(false);
            when(roleRepository.findByRoleName("ROLE_CUSTOMER")).thenReturn(Optional.of(customerRole));
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

            AdminUserResponse response = adminUserService.updateUser(2L, request, 1L);

            assertThat(response.getFullName()).isEqualTo("Nguyễn Văn Khách VIP");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("updateUser_SelfOperation_ThrowsException: Admin tries to remove own admin role -> blocked")
        void updateUser_SelfOperation_ThrowsException() {
            AdminUserUpdateRequest request = AdminUserUpdateRequest.builder()
                    .fullName("Super Admin")
                    .roles(Set.of("ROLE_STAFF"))
                    .status("active")
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testAdmin));

            assertThatThrownBy(() -> adminUserService.updateUser(1L, request, 1L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Không thể tự gỡ vai trò Quản trị viên");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("updateUser_RemoveAdminRoleFromLastActiveAdmin_ThrowsException: Demoting last active admin -> blocked")
        void updateUser_RemoveAdminRoleFromLastActiveAdmin_ThrowsException() {
            User otherAdmin = User.builder()
                    .userId(4L)
                    .fullName("Second Admin")
                    .status(UserStatus.ACTIVE)
                    .roles(new HashSet<>(Set.of(adminRole)))
                    .build();

            AdminUserUpdateRequest request = AdminUserUpdateRequest.builder()
                    .fullName("Second Admin Demoted")
                    .roles(Set.of("ROLE_STAFF"))
                    .status("active")
                    .build();

            when(userRepository.findById(4L)).thenReturn(Optional.of(otherAdmin));
            when(userRepository.countActiveAdmins()).thenReturn(1L); // Only 1 active admin in system!

            assertThatThrownBy(() -> adminUserService.updateUser(4L, request, 1L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("hoạt động cuối cùng");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("updateUser_MultipleActiveAdmins_Success: Demoting an admin when 2 active admins exist -> allowed")
        void updateUser_MultipleActiveAdmins_Success() {
            User otherAdmin = User.builder()
                    .userId(4L)
                    .fullName("Second Admin")
                    .status(UserStatus.ACTIVE)
                    .roles(new HashSet<>(Set.of(adminRole)))
                    .build();

            AdminUserUpdateRequest request = AdminUserUpdateRequest.builder()
                    .fullName("Second Admin Now Staff")
                    .roles(Set.of("ROLE_STAFF"))
                    .status("active")
                    .build();

            when(userRepository.findById(4L)).thenReturn(Optional.of(otherAdmin));
            when(userRepository.countActiveAdmins()).thenReturn(2L); // 2 active admins exist!
            when(roleRepository.findByRoleName("ROLE_STAFF")).thenReturn(Optional.of(staffRole));
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

            AdminUserResponse response = adminUserService.updateUser(4L, request, 1L);

            assertThat(response.getRoles()).contains("ROLE_STAFF");
            verify(userRepository).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("Status Update & Active Admin Guard Operations")
    class StatusTests {

        @Test
        @DisplayName("updateUserStatus should succeed when banning normal customer")
        void updateUserStatus_Customer_Success() {
            AdminUserStatusRequest request = AdminUserStatusRequest.builder().status("banned").build();

            when(userRepository.findById(2L)).thenReturn(Optional.of(testCustomer));
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

            AdminUserResponse response = adminUserService.updateUserStatus(2L, request, 1L);

            assertThat(response.getStatus()).isEqualTo("banned");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("updateUserStatus_SelfOperation_ThrowsException: Admin locks self -> blocked")
        void updateUserStatus_SelfOperation_ThrowsException() {
            AdminUserStatusRequest request = AdminUserStatusRequest.builder().status("banned").build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testAdmin));

            assertThatThrownBy(() -> adminUserService.updateUserStatus(1L, request, 1L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Không thể tự khóa");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("updateUserStatus_LastActiveAdminToBanned_ThrowsException: Locking last active admin -> blocked")
        void updateUserStatus_LastActiveAdminToBanned_ThrowsException() {
            User otherAdmin = User.builder()
                    .userId(5L)
                    .fullName("Target Admin")
                    .status(UserStatus.ACTIVE)
                    .roles(new HashSet<>(Set.of(adminRole)))
                    .build();

            AdminUserStatusRequest request = AdminUserStatusRequest.builder().status("inactive").build();

            when(userRepository.findById(5L)).thenReturn(Optional.of(otherAdmin));
            when(userRepository.countActiveAdmins()).thenReturn(1L);

            assertThatThrownBy(() -> adminUserService.updateUserStatus(5L, request, 1L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("hoạt động cuối cùng");

            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Password Reset & Delete Operations")
    class PasswordAndDelTests {

        @Test
        @DisplayName("resetUserPassword should encode and update password")
        void resetUserPassword_Success() {
            AdminUserPasswordResetRequest request = AdminUserPasswordResetRequest.builder()
                    .newPassword("NewSecurePwd#999")
                    .build();

            when(userRepository.findById(2L)).thenReturn(Optional.of(testCustomer));
            when(passwordEncoder.encode("NewSecurePwd#999")).thenReturn("hashed_new_pwd");

            adminUserService.resetUserPassword(2L, request);

            verify(passwordEncoder).encode("NewSecurePwd#999");
            verify(userRepository).save(testCustomer);
        }

        @Test
        @DisplayName("deleteUser should succeed for normal user")
        void deleteUser_Success() {
            when(userRepository.findById(2L)).thenReturn(Optional.of(testCustomer));

            adminUserService.deleteUser(2L, 1L);

            verify(userRepository).delete(testCustomer);
        }

        @Test
        @DisplayName("deleteUser_SelfOperation_ThrowsException: Admin deletes self -> blocked")
        void deleteUser_SelfOperation_ThrowsException() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testAdmin));

            assertThatThrownBy(() -> adminUserService.deleteUser(1L, 1L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Không thể tự xóa");

            verify(userRepository, never()).delete(any());
        }

        @Test
        @DisplayName("deleteUser_LastActiveAdmin_ThrowsException: Deleting last active admin -> blocked")
        void deleteUser_LastActiveAdmin_ThrowsException() {
            User targetAdmin = User.builder()
                    .userId(6L)
                    .fullName("Another Admin")
                    .status(UserStatus.ACTIVE)
                    .roles(new HashSet<>(Set.of(adminRole)))
                    .build();

            when(userRepository.findById(6L)).thenReturn(Optional.of(targetAdmin));
            when(userRepository.countActiveAdmins()).thenReturn(1L);

            assertThatThrownBy(() -> adminUserService.deleteUser(6L, 1L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("hoạt động cuối cùng");

            verify(userRepository, never()).delete(any());
        }

        @Test
        @DisplayName("deleteUser_MultipleActiveAdmins_Success: Deleting an admin when 2 active admins exist -> allowed")
        void deleteUser_MultipleActiveAdmins_Success() {
            User targetAdmin = User.builder()
                    .userId(6L)
                    .fullName("Second Admin To Delete")
                    .status(UserStatus.ACTIVE)
                    .roles(new HashSet<>(Set.of(adminRole)))
                    .build();

            when(userRepository.findById(6L)).thenReturn(Optional.of(targetAdmin));
            when(userRepository.countActiveAdmins()).thenReturn(2L); // 2 active admins exist!

            adminUserService.deleteUser(6L, 1L);

            verify(userRepository).delete(targetAdmin);
        }
    }
}
