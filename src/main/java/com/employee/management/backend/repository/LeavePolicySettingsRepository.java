package com.employee.management.backend.repository;

import com.employee.management.backend.Entity.LeavePolicySettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LeavePolicySettingsRepository extends JpaRepository<LeavePolicySettings, Long> {
    Optional<LeavePolicySettings> findByClientId(Long clientId);
}
