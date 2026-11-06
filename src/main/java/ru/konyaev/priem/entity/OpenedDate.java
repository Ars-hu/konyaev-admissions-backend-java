package ru.konyaev.priem.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "opened_date")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OpenedDate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String date;
}
