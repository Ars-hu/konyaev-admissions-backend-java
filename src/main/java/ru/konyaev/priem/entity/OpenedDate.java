package ru.konyaev.priem.entity;

import jakarta.persistence.*;

/**
 * Выходной день (суббота или воскресенье), специально открытый для приёма.
 */
@Entity
@Table(name = "opened_date")
public class OpenedDate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String date;

    public OpenedDate() {
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
