package com.store.dto.user.admin;

import com.store.entity.user.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminUserResponse {

    private Long userId;
    private String fullName;
    private String email;
    private String phone;
    private String avatarUrl;
    private String gender;
    private LocalDate birthDate;
    private String status;
    private Boolean emailVerified;
    private String provider;
    private List<String> roles;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long totalOrders;
    private BigDecimal totalSpend;

    public static AdminUserResponse fromEntity(User user, Long totalOrders, BigDecimal totalSpend) {
        if (user == null) return null;

        List<String> roleNames = user.getRoles() != null
                ? user.getRoles().stream().map(r -> r.getRoleName()).sorted().toList()
                : Collections.emptyList();

        return AdminUserResponse.builder()
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .gender(user.getGender() != null ? user.getGender().name().toLowerCase() : null)
                .birthDate(user.getBirthDate())
                .status(user.getStatus() != null ? user.getStatus().name().toLowerCase() : "active")
                .emailVerified(user.getEmailVerified())
                .provider(user.getProvider() != null ? user.getProvider().name().toLowerCase() : "local")
                .roles(roleNames)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .totalOrders(totalOrders != null ? totalOrders : 0L)
                .totalSpend(totalSpend != null ? totalSpend : BigDecimal.ZERO)
                .build();
    }

    public static AdminUserResponse fromEntity(User user) {
        return fromEntity(user, 0L, BigDecimal.ZERO);
    }
}
