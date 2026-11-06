package ru.konyaev.priem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.konyaev.priem.entity.DailyLimit;

import java.util.List;
import java.util.Optional;

@Repository
public interface DailyLimitRepository extends JpaRepository<DailyLimit, Long> {
    Optional<DailyLimit> findByDate(String date);
    List<DailyLimit> findByMaxRegistrationsGreaterThan(Integer minRegistrations);
}
