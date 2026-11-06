package ru.konyaev.priem.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import ru.konyaev.priem.entity.ArchivedApplication;

@Repository
public interface ArchivedApplicationRepository extends JpaRepository<ArchivedApplication, Long>, JpaSpecificationExecutor<ArchivedApplication> {
}
