package com.employee.management.backend.controller;

import com.employee.management.backend.Entity.Employee;
import com.employee.management.backend.Entity.Timesheet;
import com.employee.management.backend.Entity.WeeklyReport;
import com.employee.management.backend.repository.EmployeeRepository;
import com.employee.management.backend.repository.ProjectMembershipRepository;
import com.employee.management.backend.repository.TimesheetRepository;
import com.employee.management.backend.repository.WeeklyReportRepository;
import com.employee.management.backend.security.AuthenticatedUser;
import com.employee.management.backend.security.SecurityUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Daily work-update submissions. Whoever an employee currently reports to at submission time
 * gets to review it: if they're on an active project, it goes to that project's PM (see
 * ManagerController for the PM-side endpoints); if they're on the bench, it goes to HR (admin)
 * instead, reviewed here.
 */
@RestController
@RequestMapping("/api/timesheets")
public class TimesheetController {

    private final TimesheetRepository timesheetRepository;
    private final EmployeeRepository employeeRepository;
    private final ProjectMembershipRepository membershipRepository;
    private final WeeklyReportRepository weeklyReportRepository;

    public TimesheetController(TimesheetRepository timesheetRepository, EmployeeRepository employeeRepository,
                                ProjectMembershipRepository membershipRepository,
                                WeeklyReportRepository weeklyReportRepository) {
        this.timesheetRepository = timesheetRepository;
        this.employeeRepository = employeeRepository;
        this.membershipRepository = membershipRepository;
        this.weeklyReportRepository = weeklyReportRepository;
    }

    @PostMapping
    public ResponseEntity<?> submitTimesheet(@RequestBody TimesheetRequest request) {
        Employee employee = currentEmployee();
        if (employee == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Authentication required"));
        }
        if (request.description == null || request.description.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Description of today's work is required"));
        }

        boolean onActiveProject = membershipRepository.existsByEmployeeEmpIdAndEndDateIsNull(employee.getEmpId());

        Timesheet timesheet = new Timesheet();
        timesheet.setEmployee(employee);
        timesheet.setWorkDate(parseDateOrToday(request.workDate));
        timesheet.setDescription(request.description.trim());
        timesheet.setHoursWorked(request.hoursWorked);
        timesheet.setStatus("Pending");
        timesheet.setSubmittedTo(onActiveProject ? "PM" : "HR");
        timesheet.setCreatedAt(LocalDate.now());

        Timesheet saved = timesheetRepository.save(timesheet);
        return ResponseEntity.ok(toDTO(saved));
    }

    @GetMapping("/my")
    public ResponseEntity<?> getMyTimesheets() {
        Employee employee = currentEmployee();
        if (employee == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Authentication required"));
        }
        List<TimesheetDTO> timesheets = timesheetRepository
                .findByEmployeeEmpIdOrderByWorkDateDesc(employee.getEmpId()).stream()
                .map(this::toDTO)
                .toList();
        return ResponseEntity.ok(timesheets);
    }

    // Admin/HR queue: everyone whose timesheets were routed here because they were on the bench
    // at submission time. Admin can act on any of these regardless of the employee's status now.
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<?> getHrTimesheets(@RequestParam(required = false) String status) {
        List<TimesheetDTO> timesheets = timesheetRepository
                .findBySubmittedToAndEmployeeClientIdOrderByWorkDateDesc("HR", SecurityUtils.currentClientId()).stream()
                .filter(t -> status == null || status.isBlank() || "all".equalsIgnoreCase(status)
                        || status.equalsIgnoreCase(t.getStatus()))
                .map(this::toDTO)
                .toList();
        return ResponseEntity.ok(timesheets);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateTimesheetStatusAsAdmin(@PathVariable Long id, @RequestBody StatusRequest request) {
        Timesheet timesheet = timesheetRepository.findById(id).orElse(null);
        if (timesheet == null || timesheet.getEmployee() == null || timesheet.getEmployee().getClient() == null
                || !timesheet.getEmployee().getClient().getId().equals(SecurityUtils.currentClientId())) {
            return ResponseEntity.notFound().build();
        }
        if (request.status == null || request.status.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "status is required"));
        }
        timesheet.setStatus(request.status.trim());
        Timesheet saved = timesheetRepository.save(timesheet);
        return ResponseEntity.ok(toDTO(saved));
    }

    // HR's view of the PM -> HR weekly rollups (project employees' daily updates never show up
    // here individually - only via whichever weekly report a PM has bundled them into).
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/weekly-reports")
    public ResponseEntity<?> getWeeklyReportsForHr() {
        List<ManagerController.WeeklyReportDTO> reports = weeklyReportRepository
                .findByManagerClientIdOrderByWeekStartDateDesc(SecurityUtils.currentClientId()).stream()
                .map(report -> toWeeklyReportDTO(report,
                        timesheetRepository.findByWeeklyReportIdOrderByWorkDateAsc(report.getId())))
                .toList();
        return ResponseEntity.ok(reports);
    }

    private ManagerController.WeeklyReportDTO toWeeklyReportDTO(WeeklyReport report, List<Timesheet> entries) {
        ManagerController.WeeklyReportDTO dto = new ManagerController.WeeklyReportDTO();
        dto.id = report.getId();
        Employee manager = report.getManager();
        if (manager != null) {
            dto.managerName = String.format("%s %s",
                    manager.getFirstName() == null ? "" : manager.getFirstName(),
                    manager.getLastName() == null ? "" : manager.getLastName()).trim();
        }
        dto.weekStartDate = report.getWeekStartDate() != null ? report.getWeekStartDate().toString() : null;
        dto.weekEndDate = report.getWeekEndDate() != null ? report.getWeekEndDate().toString() : null;
        dto.notes = report.getNotes();
        dto.submittedAt = report.getSubmittedAt() != null ? report.getSubmittedAt().toString() : null;
        dto.entries = entries.stream().map(this::toDTO).toList();
        return dto;
    }

    private Employee currentEmployee() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof AuthenticatedUser authenticatedUser)) {
            return null;
        }
        return employeeRepository.findById(authenticatedUser.empId()).orElse(null);
    }

    private LocalDate parseDateOrToday(String value) {
        if (value == null || value.isBlank()) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (Exception ex) {
            return LocalDate.now();
        }
    }

    private TimesheetDTO toDTO(Timesheet timesheet) {
        TimesheetDTO dto = new TimesheetDTO();
        dto.id = timesheet.getId();
        Employee employee = timesheet.getEmployee();
        if (employee != null) {
            dto.empId = employee.getEmpId();
            dto.employeeName = String.format("%s %s",
                    employee.getFirstName() == null ? "" : employee.getFirstName(),
                    employee.getLastName() == null ? "" : employee.getLastName()).trim();
        }
        dto.workDate = timesheet.getWorkDate() != null ? timesheet.getWorkDate().toString() : null;
        dto.description = timesheet.getDescription();
        dto.hoursWorked = timesheet.getHoursWorked();
        dto.status = timesheet.getStatus();
        dto.submittedTo = timesheet.getSubmittedTo();
        dto.createdAt = timesheet.getCreatedAt() != null ? timesheet.getCreatedAt().toString() : null;
        return dto;
    }

    public static class TimesheetDTO {
        public Long id;
        public Long empId;
        public String employeeName;
        public String workDate;
        public String description;
        public Double hoursWorked;
        public String status;
        public String submittedTo;
        public String createdAt;
    }

    public static class TimesheetRequest {
        public String workDate;
        public String description;
        public Double hoursWorked;
    }

    public static class StatusRequest {
        public String status;
    }
}
