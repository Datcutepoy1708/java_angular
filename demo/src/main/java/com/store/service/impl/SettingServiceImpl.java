package com.store.service.impl;

import com.store.dto.setting.SettingResponse;
import com.store.dto.setting.UpdateSettingsBatchRequest;
import com.store.entity.setting.Setting;
import com.store.entity.setting.SettingGroup;
import com.store.repository.SettingRepository;
import com.store.service.SettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettingServiceImpl implements SettingService {

    private static final Pattern NUMERIC_PATTERN = Pattern.compile("^[0-9]+$");

    private final SettingRepository settingRepository;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "systemSettings", key = "'public'")
    public Map<String, String> getPublicSettings() {
        log.debug("Fetching public settings from database");
        List<Setting> publicSettings = settingRepository.findByIsPublicTrue();
        Map<String, String> result = new HashMap<>();
        for (Setting setting : publicSettings) {
            result.put(setting.getSettingKey(), setting.getSettingValue() != null ? setting.getSettingValue() : "");
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "systemSettings", key = "'all'")
    public List<SettingResponse> getAllSettings() {
        log.debug("Fetching all settings from database");
        return settingRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<SettingGroup, List<SettingResponse>> getGroupedSettings() {
        List<SettingResponse> all = getAllSettings();
        Map<SettingGroup, List<SettingResponse>> grouped = new EnumMap<>(SettingGroup.class);
        for (SettingGroup group : SettingGroup.values()) {
            grouped.put(group, new ArrayList<>());
        }
        for (SettingResponse item : all) {
            grouped.computeIfAbsent(item.getSettingGroup(), g -> new ArrayList<>()).add(item);
        }
        return grouped;
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "systemSettings", allEntries = true)
    public void updateSettings(UpdateSettingsBatchRequest request, Long currentUserId) {
        if (request == null || request.getSettings() == null || request.getSettings().isEmpty()) {
            throw new IllegalArgumentException("Danh sách cài đặt cập nhật không được để trống");
        }

        Map<String, String> newSettings = request.getSettings();
        log.info("User {} is updating {} system settings", currentUserId, newSettings.size());

        for (Map.Entry<String, String> entry : newSettings.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue() != null ? entry.getValue().trim() : "";

            validateSettingValue(key, value);

            Setting setting = settingRepository.findBySettingKey(key).orElse(null);
            if (setting != null) {
                setting.setSettingValue(value);
                settingRepository.save(setting);
            } else {
                log.warn("Setting key '{}' not found in database, creating ad-hoc setting", key);
                Setting newSetting = Setting.builder()
                        .settingKey(key)
                        .settingValue(value)
                        .settingGroup(resolveSettingGroup(key))
                        .description("Tùy biến bởi quản trị viên")
                        .isPublic(isKeyPublicByDefault(key))
                        .build();
                settingRepository.save(newSetting);
            }
        }

        log.info("System settings updated successfully and Redis cache 'systemSettings' evicted");
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "systemSettings", allEntries = true)
    public void resetToDefaults(Long currentUserId) {
        log.info("User {} initiated resetting all system settings to default values", currentUserId);
        Map<String, String> defaults = getDefaultSettingsMap();

        for (Map.Entry<String, String> entry : defaults.entrySet()) {
            String key = entry.getKey();
            String defaultValue = entry.getValue();

            Setting setting = settingRepository.findBySettingKey(key).orElse(null);
            if (setting != null) {
                setting.setSettingValue(defaultValue);
                settingRepository.save(setting);
            }
        }

        log.info("All system settings have been restored to factory defaults");
    }

    @Override
    public boolean isMaintenanceModeActive() {
        Map<String, String> publicSettings = getPublicSettings();
        String maintenanceVal = publicSettings.getOrDefault("MAINTENANCE_MODE", "false");
        return Boolean.parseBoolean(maintenanceVal);
    }

    private void validateSettingValue(String key, String value) {
        switch (key) {
            case "FREE_SHIPPING_THRESHOLD":
            case "DEFAULT_SHIPPING_FEE":
            case "ORDER_AUTO_CANCEL_HOURS":
            case "LOW_STOCK_THRESHOLD":
            case "RETURN_WINDOW_DAYS":
                if (!NUMERIC_PATTERN.matcher(value).matches()) {
                    throw new IllegalArgumentException("Cấu hình '" + key + "' phải là số nguyên dương hợp lệ, không chứa ký tự phân cách (nhận được: '" + value + "')");
                }
                break;
            case "ENABLE_COD":
            case "ENABLE_BANK_TRANSFER":
            case "MAINTENANCE_MODE":
                if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
                    throw new IllegalArgumentException("Cấu hình '" + key + "' phải là giá trị boolean 'true' hoặc 'false' (nhận được: '" + value + "')");
                }
                break;
            default:
                // String/Text settings allow arbitrary text up to reasonable length
                break;
        }
    }

    private SettingGroup resolveSettingGroup(String key) {
        if (key.startsWith("FOOTER_")) {
            return SettingGroup.FOOTER;
        }
        if (key.startsWith("STORE_")) {
            return SettingGroup.GENERAL;
        }
        if (key.startsWith("FREE_SHIPPING") || key.startsWith("DEFAULT_SHIPPING") || key.startsWith("ENABLE_") || key.startsWith("ORDER_") || key.startsWith("RETURN_")) {
            return SettingGroup.ORDER_SHIPPING;
        }
        if (key.startsWith("META_") || key.startsWith("SEO_")) {
            return SettingGroup.SEO;
        }
        return SettingGroup.SYSTEM_NOTIFICATION;
    }

    private boolean isKeyPublicByDefault(String key) {
        return !"ORDER_AUTO_CANCEL_HOURS".equals(key) && !"LOW_STOCK_THRESHOLD".equals(key);
    }

    private SettingResponse mapToResponse(Setting setting) {
        return SettingResponse.builder()
                .settingId(setting.getSettingId())
                .settingKey(setting.getSettingKey())
                .settingValue(setting.getSettingValue())
                .settingGroup(setting.getSettingGroup())
                .description(setting.getDescription())
                .isPublic(setting.getIsPublic())
                .createdAt(setting.getCreatedAt())
                .updatedAt(setting.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public int getReturnWindowDays() {
        return settingRepository.findBySettingKey("RETURN_WINDOW_DAYS")
                .map(Setting::getSettingValue)
                .map(val -> {
                    try {
                        return Integer.parseInt(val.trim());
                    } catch (NumberFormatException e) {
                        return 14;
                    }
                })
                .orElse(14);
    }

    private Map<String, String> getDefaultSettingsMap() {
        Map<String, String> map = new HashMap<>();
        map.put("STORE_NAME", "Complexus Computer & Technology");
        map.put("STORE_SLOGAN", "Đỉnh cao công nghệ PC Gaming & Linh kiện chính hãng");
        map.put("STORE_HOTLINE", "1800 6868");
        map.put("STORE_EMAIL", "support@complexus.vn");
        map.put("STORE_ADDRESS", "Số 123 Đường Công Nghệ, Quận Cầu Giấy, Hà Nội");
        map.put("STORE_WORKING_HOURS", "08:00 - 21:30 (Thứ 2 - Chủ Nhật)");
        map.put("FOOTER_BRAND_TITLE", "COMPLEXUS");
        map.put("FOOTER_DESCRIPTION", "Hệ thống bán lẻ máy tính, laptop gaming, linh kiện PC và phụ kiện công nghệ chính hãng hàng đầu Việt Nam.");
        map.put("FOOTER_HOTLINE", "1800 6868 (Miễn phí cuộc gọi, 8:00 - 21:30)");
        map.put("FOOTER_EMAIL", "support@complexus.vn");
        map.put("FOOTER_ADDRESS", "123 Đường Công Nghệ, Quận Cầu Giấy, Hà Nội");
        map.put("FOOTER_COPYRIGHT", "© 2026 Complexus — E-commerce Platform for Computers & Components. All rights reserved.");
        map.put("FOOTER_BUSINESS_LICENSE", "GPKD số: 0109876543 do Sở KH & ĐT TP. Hà Nội cấp ngày 15/01/2020.");
        map.put("FOOTER_FACEBOOK_URL", "https://facebook.com/complexus.tech");
        map.put("FOOTER_YOUTUBE_URL", "https://youtube.com/@complexus_tech");
        map.put("FOOTER_TIKTOK_URL", "https://tiktok.com/@complexus_tech");
        map.put("FREE_SHIPPING_THRESHOLD", "5000000");
        map.put("DEFAULT_SHIPPING_FEE", "35000");
        map.put("ORDER_AUTO_CANCEL_HOURS", "24");
        map.put("ENABLE_COD", "true");
        map.put("ENABLE_BANK_TRANSFER", "true");
        map.put("META_TITLE", "Complexus - Siêu thị Máy tính, Laptop Gaming & Linh kiện PC Chính Hãng");
        map.put("META_DESCRIPTION", "Chuyên cung cấp máy tính để bàn, laptop gaming, card màn hình VGA RTX 40-series, CPU Intel Gen 14, AMD Ryzen chính hãng giá tốt nhất.");
        map.put("LOW_STOCK_THRESHOLD", "5");
        map.put("MAINTENANCE_MODE", "false");
        map.put("RETURN_WINDOW_DAYS", "14");
        return map;
    }
}
