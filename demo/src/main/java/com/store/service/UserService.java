package com.store.service;

import com.store.dto.user.ChangePasswordRequest;
import com.store.dto.user.UpdateProfileRequest;
import com.store.dto.user.UserProfileResponse;

public interface UserService {

    UserProfileResponse getUserProfile(Long userId);

    UserProfileResponse updateUserProfile(Long userId, UpdateProfileRequest request);

    void changePassword(Long userId, ChangePasswordRequest request);
}
