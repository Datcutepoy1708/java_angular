package com.store.repository;

import com.store.entity.setting.Setting;
import com.store.entity.setting.SettingGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SettingRepository extends JpaRepository<Setting, Long> {

    Optional<Setting> findBySettingKey(String settingKey);

    List<Setting> findBySettingGroup(SettingGroup settingGroup);

    List<Setting> findByIsPublicTrue();

    boolean existsBySettingKey(String settingKey);
}
