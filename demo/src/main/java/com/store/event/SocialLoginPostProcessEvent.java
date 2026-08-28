package com.store.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocialLoginPostProcessEvent {

    private Long userId;
    private String email;
    private String fullName;
    private String provider;
    private String providerId;
    private boolean preHijackSuspected;
    private boolean isNewUser;
    private String clientIp;
    private String userAgent;
}
