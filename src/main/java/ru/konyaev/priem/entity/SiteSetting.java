package ru.konyaev.priem.entity;

import jakarta.persistence.*;

/**
 * Глобальные настройки системы (например, ключ "advance_hours" со значением "24").
 */
@Entity
@Table(name = "site_settings")
public class SiteSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "\"key\"", unique = true, nullable = false, length = 50)
    private String key;

    @Column(name = "\"value\"", nullable = false, length = 100)
    private String value;

    public SiteSetting() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
