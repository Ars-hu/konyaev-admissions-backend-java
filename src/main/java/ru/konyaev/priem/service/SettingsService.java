package ru.konyaev.priem.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.konyaev.priem.dto.response.SiteSettingsResponse;
import ru.konyaev.priem.entity.SiteSetting;
import ru.konyaev.priem.repository.SiteSettingRepository;

@Service
@RequiredArgsConstructor
public class SettingsService {

    private final SiteSettingRepository settingRepository;

    public String get(String key, String defaultValue) {
        return settingRepository.findByKey(key)
                .map(SiteSetting::getValue)
                .orElse(defaultValue);
    }

    public int getAdvanceHours() {
        try {
            return Integer.parseInt(get("advance_hours", "24"));
        } catch (NumberFormatException e) {
            return 24;
        }
    }

    @Transactional
    public void set(String key, String value) {
        SiteSetting setting = settingRepository.findByKey(key)
                .orElseGet(() -> SiteSetting.builder().key(key).build());
        setting.setValue(value);
        settingRepository.save(setting);
    }

    @Transactional
    public SiteSettingsResponse updateAdvanceHours(int advanceHours) {
        if (advanceHours < 0) {
            throw new IllegalArgumentException("Часы не могут быть отрицательными");
        }
        set("advance_hours", String.valueOf(advanceHours));
        return SiteSettingsResponse.builder().advanceHours(advanceHours).build();
    }

    public SiteSettingsResponse getSettings() {
        return SiteSettingsResponse.builder().advanceHours(getAdvanceHours()).build();
    }
}
