package com.store.service;

import com.store.dto.setting.SettingResponse;
import com.store.dto.setting.UpdateSettingsBatchRequest;
import com.store.entity.setting.SettingGroup;

import java.util.List;
import java.util.Map;

public interface SettingService {

    Map<String, String> getPublicSettings();

    List<SettingResponse> getAllSettings();

    Map<SettingGroup, List<SettingResponse>> getGroupedSettings();

    void updateSettings(UpdateSettingsBatchRequest request, Long currentUserId);

    void resetToDefaults(Long currentUserId);

    boolean isMaintenanceModeActive();

    int getReturnWindowDays();
}
