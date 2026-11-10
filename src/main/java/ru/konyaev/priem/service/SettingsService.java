package ru.konyaev.priem.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.konyaev.priem.dto.response.SiteSettingsResponse;
import ru.konyaev.priem.entity.SiteSetting;
import ru.konyaev.priem.repository.SiteSettingRepository;

/**
 * Сервис для чтения и изменения глобальных настроек системы (таблица site_settings).
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

    /** Возвращает количество часов, за которое нужно записываться на приём (по умолчанию 24). */
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
    public SiteSettingsResponse updateAdvanceHours(int advanceHours) {
        if (advanceHours < 0) {
            throw new IllegalArgumentException("Часы не могут быть отрицательными");
        }
        set("advance_hours", String.valueOf(advanceHours));
        return new SiteSettingsResponse(advanceHours);
    }

    public SiteSettingsResponse getSettings() {
        return new SiteSettingsResponse(getAdvanceHours());
    }
}
