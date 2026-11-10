package ru.konyaev.priem.entity;

import jakarta.persistence.*;

/**
 * Заблокированная для записи дата (праздники или неприёмные дни).
 */
@Entity
@Table(name = "blocked_date")
public class BlockedDate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String date;

    public BlockedDate() {
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
}
