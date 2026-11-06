package ru.konyaev.priem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.konyaev.priem.entity.SlotConfig;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface SlotConfigRepository extends JpaRepository<SlotConfig, Long> {
    List<SlotConfig> findByDate(String date);
    Optional<SlotConfig> findByDateAndTime(String date, String time);
    List<SlotConfig> findByDateIn(Collection<String> dates);
}
