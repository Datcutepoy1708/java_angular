package com.store.controller;

import com.store.dto.response.ApiResponse;
import com.store.dto.setting.SettingResponse;
import com.store.dto.setting.UpdateSettingsBatchRequest;
import com.store.entity.setting.SettingGroup;
import com.store.security.CustomUserDetails;
import com.store.service.SettingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettingControllerTest {

    @Mock
    private SettingService settingService;

    @InjectMocks
    private SettingController settingController;

    private Map<String, String> publicSettings;

    @BeforeEach
    void setUp() {
        publicSettings = new HashMap<>();
        publicSettings.put("STORE_NAME", "Complexus Computer");
        publicSettings.put("FREE_SHIPPING_THRESHOLD", "5000000");
    }

    @Test
    @DisplayName("getPublicSettings returns 200 OK and public map")
    void getPublicSettings_returns200() {
        when(settingService.getPublicSettings()).thenReturn(publicSettings);

        ResponseEntity<ApiResponse<Map<String, String>>> response = settingController.getPublicSettings();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData()).containsEntry("STORE_NAME", "Complexus Computer");
    }

    @Test
    @DisplayName("getAllSettings returns 200 OK and list of settings")
    void getAllSettings_returns200() {
        SettingResponse item = SettingResponse.builder()
                .settingId(1L)
                .settingKey("STORE_NAME")
                .settingValue("Complexus")
                .settingGroup(SettingGroup.GENERAL)
                .isPublic(true)
                .build();

        when(settingService.getAllSettings()).thenReturn(Collections.singletonList(item));

        ResponseEntity<ApiResponse<List<SettingResponse>>> response = settingController.getAllSettings();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).hasSize(1);
    }

    @Test
    @DisplayName("updateSettings invokes service with batch and returns 200 OK")
    void updateSettings_success() {
        UpdateSettingsBatchRequest request = UpdateSettingsBatchRequest.builder()
                .settings(publicSettings)
                .build();

        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getUserId()).thenReturn(1L);

        ResponseEntity<ApiResponse<Void>> response = settingController.updateSettings(request, userDetails);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(settingService).updateSettings(request, 1L);
    }

    @Test
    @DisplayName("resetDefaults invokes service reset and returns 200 OK")
    void resetDefaults_success() {
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getUserId()).thenReturn(1L);

        ResponseEntity<ApiResponse<Void>> response = settingController.resetDefaults(userDetails);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(settingService).resetToDefaults(1L);
    }
}
