package ru.konyaev.priem.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "slot_config", uniqueConstraints = {
        @UniqueConstraint(name = "uq_slot_date_time", columnNames = {"date", "time"})
}, indexes = {
        @Index(name = "idx_slot_date", columnList = "date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SlotConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String date;

    @Column(nullable = false, length = 10)
    private String time;

    @Column(name = "max_capacity")
    @Builder.Default
    private Integer maxCapacity = 3;

    @Column(name = "is_blocked")
    @Builder.Default
    private Boolean isBlocked = false;
}
