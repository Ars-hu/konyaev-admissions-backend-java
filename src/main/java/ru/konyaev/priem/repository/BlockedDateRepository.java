package ru.konyaev.priem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.konyaev.priem.entity.BlockedDate;

import java.util.Optional;

@Repository
public interface BlockedDateRepository extends JpaRepository<BlockedDate, Long> {
    Optional<BlockedDate> findByDate(String date);
    boolean existsByDate(String date);
    void deleteByDate(String date);
}
