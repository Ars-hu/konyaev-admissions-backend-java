package ru.konyaev.priem.entity;

import jakarta.persistence.*;

/**
 * Максимальный лимит записей на выбранный день.
 */
@Entity
@Table(name = "daily_limit")
public class DailyLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String date;

    @Column(name = "max_registrations", nullable = false)
    private Integer maxRegistrations = 0;

    public DailyLimit() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public Integer getMaxRegistrations() {
        return maxRegistrations;
    }

    public void setMaxRegistrations(Integer maxRegistrations) {
        this.maxRegistrations = maxRegistrations;
    }
}
