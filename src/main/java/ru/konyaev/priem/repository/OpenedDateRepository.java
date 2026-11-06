package ru.konyaev.priem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.konyaev.priem.entity.OpenedDate;

import java.util.Optional;

@Repository
public interface OpenedDateRepository extends JpaRepository<OpenedDate, Long> {
    Optional<OpenedDate> findByDate(String date);
    boolean existsByDate(String date);
    void deleteByDate(String date);
}
