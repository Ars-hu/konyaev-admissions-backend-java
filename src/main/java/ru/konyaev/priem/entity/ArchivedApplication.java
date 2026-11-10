package ru.konyaev.priem.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Таблица архивных заявлений (переносятся сюда после прохождения времени приёма).
 */
@Entity
@Table(name = "archived_application", indexes = {
        @Index(name = "idx_arch_reg_date", columnList = "registration_date")
})
public class ArchivedApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "original_id", nullable = false)
    private Long originalId;

    @Column(nullable = false, length = 255)
    private String fio;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(name = "registration_date", nullable = false, length = 20)
    private String registrationDate;

    @Column(name = "registration_time", nullable = false, length = 10)
    private String registrationTime;

    @Column(nullable = false, length = 20)
    private String status = "pending";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "archived_at", nullable = false)
    private Instant archivedAt = Instant.now();

    public ArchivedApplication() {
    }

    @PrePersist
    public void prePersist() {
        if (this.archivedAt == null) {
            this.archivedAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOriginalId() {
        return originalId;
    }

    public void setOriginalId(Long originalId) {
        this.originalId = originalId;
    }

    public String getFio() {
        return fio;
    }

    public void setFio(String fio) {
        this.fio = fio;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(String registrationDate) {
        this.registrationDate = registrationDate;
    }

    public String getRegistrationTime() {
        return registrationTime;
    }

    public void setRegistrationTime(String registrationTime) {
        this.registrationTime = registrationTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getArchivedAt() {
        return archivedAt;
    }

    public void setArchivedAt(Instant archivedAt) {
        this.archivedAt = archivedAt;
    }
}
