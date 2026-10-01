package com.employee.management.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.employee.management.backend.Entity.EducationDetails;

@Repository
public interface EducationDetailsRepository extends JpaRepository<EducationDetails, Long> {
}
