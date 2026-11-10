package ru.konyaev.priem.entity;

import jakarta.persistence.*;

/**
 * Индивидуальная настройка вместимости и блокировки конкретного слота (дата + время).
 */
@Entity
@Table(name = "slot_config", uniqueConstraints = {
        @UniqueConstraint(name = "uq_slot_date_time", columnNames = {"date", "time"})
}, indexes = {
        @Index(name = "idx_slot_date", columnList = "date")
})
public class SlotConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String date;

    @Column(nullable = false, length = 10)
    private String time;

    @Column(name = "max_capacity")
    private Integer maxCapacity = 3;

    @Column(name = "is_blocked")
    private Boolean isBlocked = false;

    public SlotConfig() {
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

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public Integer getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxCapacity(Integer maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    public Boolean getIsBlocked() {
        return isBlocked;
    }

    public void setIsBlocked(Boolean isBlocked) {
        this.isBlocked = isBlocked;
    }
}
