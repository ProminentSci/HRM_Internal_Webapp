package com.employee.management.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.employee.management.backend.Entity.LeaveHistory;

import java.util.List;

@Repository
public interface LeaveHistoryRepository extends JpaRepository<LeaveHistory, Long> {
    List<LeaveHistory> findByEmployeeEmpId(Long empId);
    List<LeaveHistory> findByEmployeeEmpIdAndYear(Long empId, Integer year);
}
