package ru.konyaev.priem.entity;

import jakarta.persistence.*;

/**
 * Разрешённый для записи месяц приёмной кампании (год + номер месяца).
 */
@Entity
@Table(name = "allowed_month", uniqueConstraints = {
        @UniqueConstraint(name = "uq_allowed_month", columnNames = {"\"year\"", "\"month\""})
})
public class AllowedMonth {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "\"year\"", nullable = false)
    private Integer year;

    @Column(name = "\"month\"", nullable = false)
    private Integer month;

    public AllowedMonth() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Integer getMonth() {
        return month;
    }

    public void setMonth(Integer month) {
        this.month = month;
    }
}
