package com.store.dto.setting;

import com.store.entity.setting.SettingGroup;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettingResponse {
    private Long settingId;
    private String settingKey;
    private String settingValue;
    private SettingGroup settingGroup;
    private String description;
    private Boolean isPublic;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
