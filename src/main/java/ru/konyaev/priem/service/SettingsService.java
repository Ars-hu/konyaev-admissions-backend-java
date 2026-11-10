package ru.konyaev.priem.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.konyaev.priem.entity.SiteSetting;
import ru.konyaev.priem.repository.SiteSettingRepository;

import java.util.Map;

/**
 * Сервис для чтения и сохранения глобальных настроек (таблица site_settings).
 */
@Service
public class SettingsService {

    private final SiteSettingRepository settingRepository;

    @Autowired
    public SettingsService(SiteSettingRepository settingRepository) {
        this.settingRepository = settingRepository;
    }

    public String get(String key, String defaultValue) {
        SiteSetting setting = settingRepository.findByKey(key).orElse(null);
        if (setting != null && setting.getValue() != null) {
            return setting.getValue();
        }
        return defaultValue;
    }

    /** Возвращает правило заблаговременной записи (по умолчанию 24 часа). */
    public int getAdvanceHours() {
        try {
            return Integer.parseInt(get("advance_hours", "24"));
        } catch (NumberFormatException e) {
            return 24;
        }
    }

    @Transactional
    public void set(String key, String value) {
        SiteSetting setting = settingRepository.findByKey(key).orElse(null);
        if (setting == null) {
            setting = new SiteSetting();
            setting.setKey(key);
        }
        setting.setValue(value);
        settingRepository.save(setting);
    }

    @Transactional
    public Map<String, Object> updateAdvanceHours(int advanceHours) {
        if (advanceHours < 0 || advanceHours > 720) {
            throw new IllegalArgumentException("Часы должны быть в диапазоне от 0 до 720");
        }
        set("advance_hours", String.valueOf(advanceHours));
        return Map.of("advance_hours", advanceHours);
    }

    public Map<String, Object> getSettings() {
        return Map.of("advance_hours", getAdvanceHours());
    }
}
