package com.employee.management.backend.repository;

import com.employee.management.backend.Entity.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface HolidayRepository extends JpaRepository<Holiday, Long> {
    Optional<Holiday> findByDateAndTitleAndClientId(LocalDate date, String title, Long clientId);

    Optional<Holiday> findByIdAndClientId(Long id, Long clientId);

    @Query("SELECT h FROM Holiday h WHERE h.clientId = :clientId AND FUNCTION('YEAR', h.date) = :year ORDER BY h.date ASC")
    List<Holiday> findByYearAndClientId(@Param("year") Integer year, @Param("clientId") Long clientId);

    @Query("SELECT DISTINCT FUNCTION('YEAR', h.date) FROM Holiday h WHERE h.clientId = :clientId ORDER BY FUNCTION('YEAR', h.date) DESC")
    List<Integer> findDistinctYearsByClientId(@Param("clientId") Long clientId);

    List<Holiday> findByClientIdAndDateBetweenOrderByDateAsc(Long clientId, LocalDate start, LocalDate end);

    List<Holiday> findAllByClientIdOrderByDateAsc(Long clientId);

    // Unscoped - kept only for AttendanceController/AttendanceReportController, which are not
    // yet tenant-scoped themselves (out of scope for this fix; see the multi-tenancy plan).
    List<Holiday> findByDateBetweenOrderByDateAsc(LocalDate start, LocalDate end);
}
