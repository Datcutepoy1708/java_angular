package com.store.service;

import com.store.dto.user.ChangePasswordRequest;
import com.store.dto.user.UpdateProfileRequest;
import com.store.dto.user.UserProfileResponse;
import com.store.entity.user.Gender;
import com.store.entity.user.Role;
import com.store.entity.user.User;
import com.store.entity.user.UserStatus;
import com.store.exception.DuplicateResourceException;
import com.store.exception.ResourceNotFoundException;
import com.store.repository.UserRepository;
import com.store.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        Role role = Role.builder().roleId(1).roleName("ROLE_CUSTOMER").build();
        sampleUser = User.builder()
                .userId(1L)
                .fullName("Nguyễn Văn A")
                .email("user@example.com")
                .phone("0987654321")
                .passwordHash("$2a$10$hashedPassword")
                .gender(Gender.MALE)
                .birthDate(LocalDate.of(1995, 5, 15))
                .status(UserStatus.ACTIVE)
                .roles(Set.of(role))
                .build();
    }

    @Test
    @DisplayName("getUserProfile should return mapped user profile response")
    void testGetUserProfile_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

        UserProfileResponse response = userService.getUserProfile(1L);

        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getFullName()).isEqualTo("Nguyễn Văn A");
        assertThat(response.getEmail()).isEqualTo("user@example.com");
        assertThat(response.getRoles()).contains("ROLE_CUSTOMER");
    }

    @Test
    @DisplayName("getUserProfile should throw ResourceNotFoundException when user does not exist")
    void testGetUserProfile_NotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserProfile(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("updateUserProfile should successfully update fields")
    void testUpdateUserProfile_Success() {
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .fullName("Nguyễn Văn B")
                .phone("0912345678")
                .gender(Gender.OTHER)
                .birthDate(LocalDate.of(1996, 6, 20))
                .avatarUrl("https://example.com/avatar.jpg")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userRepository.findByPhone("0912345678")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfileResponse response = userService.updateUserProfile(1L, request);

        assertThat(response.getFullName()).isEqualTo("Nguyễn Văn B");
        assertThat(response.getPhone()).isEqualTo("0912345678");
        assertThat(response.getGender()).isEqualTo(Gender.OTHER);
        assertThat(response.getAvatarUrl()).isEqualTo("https://example.com/avatar.jpg");
    }

    @Test
    @DisplayName("updateUserProfile should throw DuplicateResourceException when phone is already taken by another user")
    void testUpdateUserProfile_DuplicatePhone() {
        User anotherUser = User.builder().userId(2L).phone("0912345678").build();
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .fullName("Nguyễn Văn B")
                .phone("0912345678")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userRepository.findByPhone("0912345678")).thenReturn(Optional.of(anotherUser));

        assertThatThrownBy(() -> userService.updateUserProfile(1L, request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Số điện thoại này đã được sử dụng");
    }

    @Test
    @DisplayName("changePassword should succeed when old password matches and new password differs")
    void testChangePassword_Success() {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .oldPassword("OldPass@123")
                .newPassword("NewPass@456")
                .confirmPassword("NewPass@456")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("OldPass@123", "$2a$10$hashedPassword")).thenReturn(true);
        when(passwordEncoder.matches("NewPass@456", "$2a$10$hashedPassword")).thenReturn(false);
        when(passwordEncoder.encode("NewPass@456")).thenReturn("$2a$10$newHashedPassword");

        userService.changePassword(1L, request);

        verify(userRepository).save(sampleUser);
        assertThat(sampleUser.getPasswordHash()).isEqualTo("$2a$10$newHashedPassword");
    }

    @Test
    @DisplayName("changePassword should throw IllegalArgumentException when old password is wrong")
    void testChangePassword_WrongOldPassword() {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .oldPassword("WrongPass")
                .newPassword("NewPass@456")
                .confirmPassword("NewPass@456")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("WrongPass", "$2a$10$hashedPassword")).thenReturn(false);

        assertThatThrownBy(() -> userService.changePassword(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Mật khẩu hiện tại không chính xác");
    }

    @Test
    @DisplayName("changePassword should throw IllegalArgumentException when new password matches old password")
    void testChangePassword_SameAsOldPassword() {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .oldPassword("OldPass@123")
                .newPassword("OldPass@123")
                .confirmPassword("OldPass@123")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("OldPass@123", "$2a$10$hashedPassword")).thenReturn(true);
        when(passwordEncoder.matches("OldPass@123", "$2a$10$hashedPassword")).thenReturn(true);

        assertThatThrownBy(() -> userService.changePassword(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Mật khẩu mới không được trùng với mật khẩu hiện tại");
    }

    @Test
    @DisplayName("changePassword should throw IllegalArgumentException when confirmation does not match")
    void testChangePassword_ConfirmMismatch() {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .oldPassword("OldPass@123")
                .newPassword("NewPass@456")
                .confirmPassword("DifferentPass@789")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("OldPass@123", "$2a$10$hashedPassword")).thenReturn(true);
        when(passwordEncoder.matches("NewPass@456", "$2a$10$hashedPassword")).thenReturn(false);

        assertThatThrownBy(() -> userService.changePassword(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Mật khẩu xác nhận không khớp");
    }
}
