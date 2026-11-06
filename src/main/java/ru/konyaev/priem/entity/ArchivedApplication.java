package ru.konyaev.priem.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "archived_application", indexes = {
        @Index(name = "idx_arch_reg_date", columnList = "registration_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
    @Builder.Default
    private String status = "pending";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "archived_at", nullable = false)
    @Builder.Default
    private Instant archivedAt = Instant.now();

    @PrePersist
    public void prePersist() {
        if (this.archivedAt == null) {
            this.archivedAt = Instant.now();
        }
    }
}
