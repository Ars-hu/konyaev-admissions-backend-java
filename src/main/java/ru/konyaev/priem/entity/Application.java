package ru.konyaev.priem.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "application", indexes = {
        @Index(name = "idx_app_reg_date", columnList = "registration_date"),
        @Index(name = "idx_app_phone", columnList = "phone"),
        @Index(name = "idx_app_email", columnList = "email")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "pending";

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.status == null) {
            this.status = "pending";
        }
    }
}
