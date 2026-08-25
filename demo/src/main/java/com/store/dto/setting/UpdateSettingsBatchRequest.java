package com.store.dto.setting;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSettingsBatchRequest {

    @NotEmpty(message = "Danh sách cài đặt không được để trống")
    private Map<String, String> settings;
}
