package ru.konyaev.priem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.konyaev.priem.entity.AllowedMonth;

import java.util.List;
import java.util.Optional;

@Repository
public interface AllowedMonthRepository extends JpaRepository<AllowedMonth, Long> {
    List<AllowedMonth> findAllByOrderByYearAscMonthAsc();
    Optional<AllowedMonth> findByYearAndMonth(Integer year, Integer month);
    boolean existsByYearAndMonth(Integer year, Integer month);
}
