package ru.konyaev.priem.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "blocked_date")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlockedDate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String date;
}
