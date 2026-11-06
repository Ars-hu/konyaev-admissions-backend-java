package ru.konyaev.priem.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.konyaev.priem.entity.Application;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long>, JpaSpecificationExecutor<Application> {

    List<Application> findByRegistrationDate(String registrationDate);

    List<Application> findByRegistrationDateOrderByRegistrationDateAscRegistrationTimeAsc(String registrationDate);

    List<Application> findAllByOrderByRegistrationDateAscRegistrationTimeAsc();

    long countByRegistrationDate(String registrationDate);

    long countByRegistrationDateAndRegistrationTime(String registrationDate, String registrationTime);

    @Query("SELECT a FROM Application a WHERE a.registrationDate = :date AND (a.phone = :phone OR a.email = :email)")
    Optional<Application> findExistingByDateAndPhoneOrEmail(
            @Param("date") String date,
            @Param("phone") String phone,
            @Param("email") String email
    );

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

    @Query("SELECT a.registrationDate as date, a.registrationTime as time, COUNT(a.id) as count " +
           "FROM Application a GROUP BY a.registrationDate, a.registrationTime")
    List<SlotCountView> countGroupedByDateAndTime();

    boolean existsByRegistrationDateStartingWith(String prefix);
}
