package com.store.dto.response.auth;

import com.store.entity.user.AuthProvider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocialUserInfo {

    private AuthProvider provider;
    private String providerId;
    private String email;
    private String fullName;
    private String avatarUrl;
    private boolean emailVerified;
}
