package ru.konyaev.priem.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.konyaev.priem.entity.Application;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с таблицей активных заявлений (application).
 */
@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    List<Application> findByRegistrationDate(String registrationDate);

    List<Application> findByRegistrationDateOrderByRegistrationDateAscRegistrationTimeAsc(String registrationDate);

    List<Application> findAllByOrderByRegistrationDateAscRegistrationTimeAsc();

    long countByRegistrationDate(String registrationDate);

    long countByRegistrationDateAndRegistrationTime(String registrationDate, String registrationTime);

    /** Поиск заявлений по ФИО, телефону, email, дате или времени. */
    @Query("SELECT a FROM Application a WHERE " +
           "LOWER(a.fio) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(a.phone) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(a.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(a.registrationDate) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(a.registrationTime) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Application> searchApplications(@Param("search") String search, Pageable pageable);

    /** Проверка, записан ли уже человек с таким телефоном или email на указанную дату. */
    @Query("SELECT a FROM Application a WHERE a.registrationDate = :date AND (a.phone = :phone OR a.email = :email)")
    Optional<Application> findExistingByDateAndPhoneOrEmail(
            @Param("date") String date,
            @Param("phone") String phone,
            @Param("email") String email
    );

    /** Поиск просроченных заявлений, время приёма которых уже прошло. */
    @Query("SELECT a FROM Application a WHERE a.registrationDate < :today OR (a.registrationDate = :today AND a.registrationTime < :currentTime)")
    List<Application> findExpired(
            @Param("today") String today,
            @Param("currentTime") String currentTime
    );

    interface SlotCountView {
        String getDate();
        String getTime();
        Long getCount();
    }

    /** Подсчёт количества записей для каждой пары (дата, время). */
    @Query("SELECT a.registrationDate as date, a.registrationTime as time, COUNT(a.id) as count " +
           "FROM Application a GROUP BY a.registrationDate, a.registrationTime")
    List<SlotCountView> countGroupedByDateAndTime();

    boolean existsByRegistrationDateStartingWith(String prefix);
}
