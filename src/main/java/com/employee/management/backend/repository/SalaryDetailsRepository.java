package com.employee.management.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.employee.management.backend.Entity.SalaryDetails;

@Repository
public interface SalaryDetailsRepository extends JpaRepository<SalaryDetails, Long> {
}
