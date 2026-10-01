package com.employee.management.backend.repository;

import com.employee.management.backend.Entity.WeeklyReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WeeklyReportRepository extends JpaRepository<WeeklyReport, Long> {
    List<WeeklyReport> findByManagerEmpIdOrderByWeekStartDateDesc(Long managerEmpId);

    List<WeeklyReport> findAllByOrderByWeekStartDateDesc();

    List<WeeklyReport> findByManagerClientIdOrderByWeekStartDateDesc(Long clientId);
}
