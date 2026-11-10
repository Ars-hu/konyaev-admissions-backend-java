package ru.konyaev.priem.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.konyaev.priem.entity.ArchivedApplication;

/**
 * Репозиторий для работы с таблицей архивных заявлений (archived_application).
 */
@Repository
public interface ArchivedApplicationRepository extends JpaRepository<ArchivedApplication, Long> {

    /** Поиск по архивным заявлениям по ФИО, телефону, email или дате. */
    @Query("SELECT a FROM ArchivedApplication a WHERE " +
           "LOWER(a.fio) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(a.phone) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(a.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(a.registrationDate) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<ArchivedApplication> searchArchive(@Param("search") String search, Pageable pageable);
}
