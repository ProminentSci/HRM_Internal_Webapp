package com.employee.management.backend.repository;

import com.employee.management.backend.Entity.Timesheet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository
public interface TimesheetRepository extends JpaRepository<Timesheet, Long> {
    List<Timesheet> findByEmployeeEmpIdOrderByWorkDateDesc(Long empId);

    List<Timesheet> findByEmployeeEmpIdInOrderByWorkDateDesc(Collection<Long> empIds);

    List<Timesheet> findBySubmittedToOrderByWorkDateDesc(String submittedTo);

    List<Timesheet> findBySubmittedToAndEmployeeClientIdOrderByWorkDateDesc(String submittedTo, Long clientId);

    // Entries eligible for this week's PM -> HR rollup: this manager's team, dated within the
    // week, not already swept into an earlier weekly report.
    List<Timesheet> findByEmployeeEmpIdInAndWorkDateBetweenAndWeeklyReportIsNullOrderByWorkDateAsc(
            Collection<Long> empIds, LocalDate start, LocalDate end);

    List<Timesheet> findByWeeklyReportIdOrderByWorkDateAsc(Long weeklyReportId);
}
