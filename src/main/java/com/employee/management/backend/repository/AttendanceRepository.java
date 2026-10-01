package com.employee.management.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.employee.management.backend.Entity.Attendance;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    List<Attendance> findByEmployeeEmpIdOrderByIdDesc(Long empId);
    Optional<Attendance> findByEmployeeEmpIdAndDate(Long empId, String date);
    List<Attendance> findByDateStartingWith(String datePrefix);
}
