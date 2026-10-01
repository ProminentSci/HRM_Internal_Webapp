package com.employee.management.backend.controller;

import com.employee.management.backend.dto.PerformanceReportDTO;
import com.employee.management.backend.security.AuthenticatedUser;
import com.employee.management.backend.service.PerformanceReportService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// Client admin's read-only view of the performance reports their Project Managers have
// submitted. Submission itself happens through ManagerController (PM-scoped, requires
// requireProjectManager() team-checking) - this controller only ever reads, scoped to the
// admin's own client, the same way TicketController scopes tickets.
@RestController
@RequestMapping("/api/performance-reports")
public class PerformanceReportController {

    private final PerformanceReportService performanceReportService;

    public PerformanceReportController(PerformanceReportService performanceReportService) {
        this.performanceReportService = performanceReportService;
    }

    private AuthenticatedUser currentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof AuthenticatedUser authenticatedUser)) {
            throw new RuntimeException("Not authenticated");
        }
        return authenticatedUser;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<Page<PerformanceReportDTO>> getClientPerformanceReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) String employeeName) {
        Page<PerformanceReportDTO> result = performanceReportService.getReportsForClientPage(
                currentUser().clientId(), month, employeeName, PageRequest.of(Math.max(page, 0), Math.max(size, 1)));
        return ResponseEntity.ok(result);
    }
}
