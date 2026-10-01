package com.employee.management.backend.repository;

import com.employee.management.backend.Entity.PerformanceReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PerformanceReportRepository extends JpaRepository<PerformanceReport, Long> {
    Optional<PerformanceReport> findByEmployeeEmpIdAndMonthAndYear(Long empId, Integer month, Integer year);

    List<PerformanceReport> findByManagerEmpIdOrderByYearDescMonthDesc(Long managerEmpId);

    @Query("SELECT p FROM PerformanceReport p JOIN p.employee e WHERE e.client.id = :clientId " +
            "AND (:month IS NULL OR p.month = :month) " +
            "AND (:employeeName IS NULL OR :employeeName = '' OR " +
            "  LOWER(CONCAT(COALESCE(e.firstName,''),' ',COALESCE(e.lastName,''))) LIKE LOWER(CONCAT('%',:employeeName,'%'))) " +
            "ORDER BY p.year DESC, p.month DESC")
    Page<PerformanceReport> filterForClient(@Param("clientId") Long clientId, @Param("month") Integer month,
                                             @Param("employeeName") String employeeName, Pageable pageable);
}
