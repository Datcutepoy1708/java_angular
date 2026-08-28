package com.store.dto.response.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FacebookDebugTokenResponse {

    @JsonProperty("data")
    private DebugData data;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DebugData {

        @JsonProperty("app_id")
        private String appId;

        @JsonProperty("type")
        private String type;

        @JsonProperty("application")
        private String application;

        @JsonProperty("expires_at")
        private Long expiresAt;

        @JsonProperty("is_valid")
        private boolean valid;

        @JsonProperty("user_id")
        private String userId;

        @JsonProperty("scopes")
        private List<String> scopes;
    }
}
