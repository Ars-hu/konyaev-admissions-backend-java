package ru.konyaev.priem.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "daily_limit")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String date;

    @Column(name = "max_registrations", nullable = false)
    @Builder.Default
    private Integer maxRegistrations = 0;
}
