package com.employee.management.backend.repository;

import com.employee.management.backend.Entity.PayslipSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PayslipSettingsRepository extends JpaRepository<PayslipSettings, Long> {
    Optional<PayslipSettings> findByClientId(Long clientId);
}
