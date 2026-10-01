package com.employee.management.backend.controller;

import com.employee.management.backend.dto.LeavePolicySettingsDTO;
import com.employee.management.backend.security.SecurityUtils;
import com.employee.management.backend.service.LeavePolicySettingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/leave-settings")
public class LeavePolicySettingsController {

    private final LeavePolicySettingsService leavePolicySettingsService;

    public LeavePolicySettingsController(LeavePolicySettingsService leavePolicySettingsService) {
        this.leavePolicySettingsService = leavePolicySettingsService;
    }

    // Any authenticated user - employees applying for leave need to see their org's own allocations.
    @GetMapping
    public ResponseEntity<?> getSettings() {
        return ResponseEntity.ok(leavePolicySettingsService.getSettings(SecurityUtils.currentClientId()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping
    public ResponseEntity<?> updateSettings(@RequestBody LeavePolicySettingsDTO request) {
        try {
            return ResponseEntity.ok(leavePolicySettingsService.updateSettings(SecurityUtils.currentClientId(), request));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }
}
