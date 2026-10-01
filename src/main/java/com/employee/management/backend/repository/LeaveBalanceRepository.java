package com.employee.management.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.employee.management.backend.Entity.LeaveBalance;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Long> {
    List<LeaveBalance> findByEmployeeEmpId(Long empId);
    Optional<LeaveBalance> findByEmployeeEmpIdAndLeaveType(Long empId, String leaveType);
    List<LeaveBalance> findByEmployeeClientId(Long clientId);
}
