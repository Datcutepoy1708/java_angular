package com.store.controller;

import com.store.dto.user.ChangePasswordRequest;
import com.store.dto.user.UpdateProfileRequest;
import com.store.dto.user.UserProfileResponse;
import com.store.security.CustomUserDetails;
import com.store.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.store.entity.user.User;
import com.store.entity.user.UserStatus;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private CustomUserDetails customUserDetails;

    @BeforeEach
    void setUp() {
        User user = User.builder()
                .userId(1L)
                .email("user@example.com")
                .fullName("Nguyễn Văn A")
                .passwordHash("Password@123")
                .status(UserStatus.ACTIVE)
                .build();
        customUserDetails = new CustomUserDetails(user);
    }

    @Test
    @DisplayName("GET /api/v1/users/me returns current user profile")
    void testGetMyProfile() {
        UserProfileResponse mockProfile = UserProfileResponse.builder()
                .userId(1L)
                .fullName("Nguyễn Văn A")
                .email("user@example.com")
                .build();

        when(userService.getUserProfile(1L)).thenReturn(mockProfile);

        var response = userController.getMyProfile(customUserDetails);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getFullName()).isEqualTo("Nguyễn Văn A");
    }

    @Test
    @DisplayName("PUT /api/v1/users/me updates and returns profile")
    void testUpdateMyProfile() {
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .fullName("Nguyễn Văn B")
                .build();

        UserProfileResponse updatedProfile = UserProfileResponse.builder()
                .userId(1L)
                .fullName("Nguyễn Văn B")
                .build();

        when(userService.updateUserProfile(1L, request)).thenReturn(updatedProfile);

        var response = userController.updateMyProfile(customUserDetails, request);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody().getData().getFullName()).isEqualTo("Nguyễn Văn B");
    }

    @Test
    @DisplayName("PUT /api/v1/users/me/password changes password")
    void testChangePassword() {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .oldPassword("OldPass@123")
                .newPassword("NewPass@456")
                .confirmPassword("NewPass@456")
                .build();

        var response = userController.changePassword(customUserDetails, request);

        verify(userService).changePassword(1L, request);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }
}
