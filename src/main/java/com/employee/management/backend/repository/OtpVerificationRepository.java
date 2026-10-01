package com.employee.management.backend.repository;

import com.employee.management.backend.Entity.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {
    Optional<OtpVerification> findTopByEmployee_EmpIdAndUsedFalseOrderByIdDesc(Long empId);

    @Modifying
    @Query("UPDATE OtpVerification o SET o.used = true WHERE o.employee.empId = :empId AND o.used = false")
    void invalidateActiveOtpsForEmployee(@Param("empId") Long empId);
}
