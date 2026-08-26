package com.store.service;

import com.store.dto.setting.SettingResponse;
import com.store.dto.setting.UpdateSettingsBatchRequest;
import com.store.entity.setting.Setting;
import com.store.entity.setting.SettingGroup;
import com.store.repository.SettingRepository;
import com.store.service.impl.SettingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettingServiceTest {

    @Mock
    private SettingRepository settingRepository;

    @InjectMocks
    private SettingServiceImpl settingService;

    private Setting storeNameSetting;
    private Setting freeshipSetting;
    private Setting maintenanceSetting;
    private Setting autoCancelSetting;

    @BeforeEach
    void setUp() {
        storeNameSetting = Setting.builder()
                .settingId(1L)
                .settingKey("STORE_NAME")
                .settingValue("Complexus Tech")
                .settingGroup(SettingGroup.GENERAL)
                .description("Store Name")
                .isPublic(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        freeshipSetting = Setting.builder()
                .settingId(2L)
                .settingKey("FREE_SHIPPING_THRESHOLD")
                .settingValue("5000000")
                .settingGroup(SettingGroup.ORDER_SHIPPING)
                .description("Freeship threshold")
                .isPublic(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        maintenanceSetting = Setting.builder()
                .settingId(3L)
                .settingKey("MAINTENANCE_MODE")
                .settingValue("false")
                .settingGroup(SettingGroup.SYSTEM_NOTIFICATION)
                .description("Maintenance mode")
                .isPublic(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        autoCancelSetting = Setting.builder()
                .settingId(4L)
                .settingKey("ORDER_AUTO_CANCEL_HOURS")
                .settingValue("24")
                .settingGroup(SettingGroup.ORDER_SHIPPING)
                .description("Auto cancel hours")
                .isPublic(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("getPublicSettings returns map of only public settings")
    void getPublicSettings_returnsPublicMap() {
        when(settingRepository.findByIsPublicTrue())
                .thenReturn(Arrays.asList(storeNameSetting, freeshipSetting, maintenanceSetting));

        Map<String, String> result = settingService.getPublicSettings();

        assertThat(result).hasSize(3);
        assertThat(result.get("STORE_NAME")).isEqualTo("Complexus Tech");
        assertThat(result.get("FREE_SHIPPING_THRESHOLD")).isEqualTo("5000000");
        assertThat(result.get("MAINTENANCE_MODE")).isEqualTo("false");
        assertThat(result.containsKey("ORDER_AUTO_CANCEL_HOURS")).isFalse();
    }

    @Test
    @DisplayName("getAllSettings returns all mapped responses")
    void getAllSettings_returnsList() {
        when(settingRepository.findAll())
                .thenReturn(Arrays.asList(storeNameSetting, freeshipSetting, maintenanceSetting, autoCancelSetting));

        List<SettingResponse> list = settingService.getAllSettings();

        assertThat(list).hasSize(4);
        assertThat(list.get(0).getSettingKey()).isEqualTo("STORE_NAME");
    }

    @Test
    @DisplayName("updateSettings with valid values updates existing entities")
    void updateSettings_validBatch_success() {
        Map<String, String> batch = new HashMap<>();
        batch.put("STORE_NAME", "New Complexus Flagship");
        batch.put("FREE_SHIPPING_THRESHOLD", "10000000");
        batch.put("MAINTENANCE_MODE", "true");

        UpdateSettingsBatchRequest request = UpdateSettingsBatchRequest.builder()
                .settings(batch)
                .build();

        when(settingRepository.findBySettingKey("STORE_NAME")).thenReturn(Optional.of(storeNameSetting));
        when(settingRepository.findBySettingKey("FREE_SHIPPING_THRESHOLD")).thenReturn(Optional.of(freeshipSetting));
        when(settingRepository.findBySettingKey("MAINTENANCE_MODE")).thenReturn(Optional.of(maintenanceSetting));

        settingService.updateSettings(request, 1L);

        assertThat(storeNameSetting.getSettingValue()).isEqualTo("New Complexus Flagship");
        assertThat(freeshipSetting.getSettingValue()).isEqualTo("10000000");
        assertThat(maintenanceSetting.getSettingValue()).isEqualTo("true");
        verify(settingRepository, atLeastOnce()).save(any(Setting.class));
    }

    @Test
    @DisplayName("updateSettings with invalid numeric format throws IllegalArgumentException")
    void updateSettings_invalidNumeric_throwsException() {
        Map<String, String> batch = new HashMap<>();
        batch.put("FREE_SHIPPING_THRESHOLD", "5.000.000"); // formatted string with dots

        UpdateSettingsBatchRequest request = UpdateSettingsBatchRequest.builder()
                .settings(batch)
                .build();

        assertThatThrownBy(() -> settingService.updateSettings(request, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("FREE_SHIPPING_THRESHOLD");
    }

    @Test
    @DisplayName("updateSettings with invalid boolean format throws IllegalArgumentException")
    void updateSettings_invalidBoolean_throwsException() {
        Map<String, String> batch = new HashMap<>();
        batch.put("MAINTENANCE_MODE", "yes"); // not "true" or "false"

        UpdateSettingsBatchRequest request = UpdateSettingsBatchRequest.builder()
                .settings(batch)
                .build();

        assertThatThrownBy(() -> settingService.updateSettings(request, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("MAINTENANCE_MODE");
    }

    @Test
    @DisplayName("isMaintenanceModeActive checks public settings correctly")
    void isMaintenanceModeActive_returnsBoolean() {
        when(settingRepository.findByIsPublicTrue())
                .thenReturn(Arrays.asList(storeNameSetting, maintenanceSetting));

        boolean isActive = settingService.isMaintenanceModeActive();
        assertThat(isActive).isFalse();

        maintenanceSetting.setSettingValue("true");
        boolean isNowActive = settingService.isMaintenanceModeActive();
        assertThat(isNowActive).isTrue();
    }

    @Test
    @DisplayName("getReturnWindowDays returns value from setting or defaults to 14")
    void getReturnWindowDays_returnsValueOrDefault() {
        when(settingRepository.findBySettingKey("RETURN_WINDOW_DAYS"))
                .thenReturn(Optional.of(Setting.builder().settingKey("RETURN_WINDOW_DAYS").settingValue("30").build()));

        int days = settingService.getReturnWindowDays();
        assertThat(days).isEqualTo(30);

        when(settingRepository.findBySettingKey("RETURN_WINDOW_DAYS")).thenReturn(Optional.empty());
        int defaultDays = settingService.getReturnWindowDays();
        assertThat(defaultDays).isEqualTo(14);
    }
}
