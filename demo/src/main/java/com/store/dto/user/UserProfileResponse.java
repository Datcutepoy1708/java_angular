package com.store.dto.user;

import com.store.entity.user.AuthProvider;
import com.store.entity.user.Gender;
import com.store.entity.user.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long userId;
    private String fullName;
    private String email;
    private String phone;
    private String avatarUrl;
    private Gender gender;
    private LocalDate birthDate;
    private UserStatus status;
    private Boolean emailVerified;
    private AuthProvider provider;
    private Set<String> roles;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
