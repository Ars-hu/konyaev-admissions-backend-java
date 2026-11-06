package ru.konyaev.priem.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "site_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SiteSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "\"key\"", unique = true, nullable = false, length = 50)
    private String key;

    @Column(name = "\"value\"", nullable = false, length = 100)
    private String value;
}
