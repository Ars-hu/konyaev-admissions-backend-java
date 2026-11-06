package ru.konyaev.priem.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "allowed_month", uniqueConstraints = {
        @UniqueConstraint(name = "uq_allowed_month", columnNames = {"\"year\"", "\"month\""})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AllowedMonth {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "\"year\"", nullable = false)
    private Integer year;

    @Column(name = "\"month\"", nullable = false)
    private Integer month;
}
