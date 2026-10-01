package com.employee.management.backend.controller;

import com.employee.management.backend.Entity.Employee;
import com.employee.management.backend.Entity.JobDetails;
import com.employee.management.backend.Entity.Project;
import com.employee.management.backend.Entity.ProjectMembership;
import com.employee.management.backend.Entity.Timesheet;
import com.employee.management.backend.Entity.WeeklyReport;
import com.employee.management.backend.dto.LeaveRequestDTO;
import com.employee.management.backend.dto.PerformanceReportDTO;
import com.employee.management.backend.dto.SubmitPerformanceReportDTO;
import com.employee.management.backend.dto.UpdateLeaveRequestStatusDTO;
import com.employee.management.backend.repository.AttendanceRepository;
import com.employee.management.backend.repository.EmployeeRepository;
import com.employee.management.backend.repository.ProjectMembershipRepository;
import com.employee.management.backend.repository.ProjectRepository;
import com.employee.management.backend.repository.TimesheetRepository;
import com.employee.management.backend.repository.WeeklyReportRepository;
import com.employee.management.backend.security.AuthenticatedUser;
import com.employee.management.backend.service.LeaveRequestService;
import com.employee.management.backend.service.PerformanceReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Self-service endpoints for employees whose JobDetails.positionLevel is PROJECT_MANAGER: view
 * their own team - everyone with an open ProjectMembership on any project they manage - and
 * approve/reject that team's leave requests. Scope is derived entirely from the verified JWT
 * (never a client-supplied id), and every action re-checks the target employee is actually part
 * of the caller's team before touching anything.
 */
@RestController
@RequestMapping("/api/manager")
public class ManagerController {

    private final EmployeeRepository employeeRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMembershipRepository membershipRepository;
    private final AttendanceRepository attendanceRepository;
    private final LeaveRequestService leaveRequestService;
    private final TimesheetRepository timesheetRepository;
    private final WeeklyReportRepository weeklyReportRepository;
    private final PerformanceReportService performanceReportService;

    public ManagerController(EmployeeRepository employeeRepository, ProjectRepository projectRepository,
                              ProjectMembershipRepository membershipRepository,
                              AttendanceRepository attendanceRepository, LeaveRequestService leaveRequestService,
                              TimesheetRepository timesheetRepository, WeeklyReportRepository weeklyReportRepository,
                              PerformanceReportService performanceReportService) {
        this.employeeRepository = employeeRepository;
        this.projectRepository = projectRepository;
        this.membershipRepository = membershipRepository;
        this.attendanceRepository = attendanceRepository;
        this.leaveRequestService = leaveRequestService;
        this.timesheetRepository = timesheetRepository;
        this.weeklyReportRepository = weeklyReportRepository;
        this.performanceReportService = performanceReportService;
    }

    // Cheap check the frontend uses to decide whether to even show "My Team" navigation - avoids
    // pulling the full roster just to answer "am I currently an active project manager".
    @GetMapping("/status")
    public ResponseEntity<?> getManagerStatus() {
        boolean isManager = requireProjectManager() != null;
        return ResponseEntity.ok(Map.of("isManager", isManager));
    }

    @GetMapping("/team")
    public ResponseEntity<?> getMyTeam() {
        Employee manager = requireProjectManager();
        if (manager == null) {
            return ResponseEntity.status(403).body(Map.of("error", "Project Manager access required"));
        }

        String today = LocalDate.now().toString();
        List<TeamMemberDTO> team = getManagedMembers(manager.getEmpId()).stream()
                .map(employee -> {
                    TeamMemberDTO dto = new TeamMemberDTO();
                    dto.empId = employee.getEmpId();
                    dto.name = fullName(employee);
                    JobDetails jd = employee.getJobDetails();
                    dto.designation = jd != null ? jd.getDesignation() : null;
                    dto.department = jd != null ? jd.getDepartment() : null;
                    dto.attendanceStatus = attendanceRepository.findByEmployeeEmpIdAndDate(employee.getEmpId(), today)
                            .map(a -> a.getStatus() == null ? "PRESENT" : a.getStatus())
                            .orElse("ABSENT");
                    return dto;
                })
                .sorted(Comparator.comparing(m -> m.name == null ? "" : m.name))
                .toList();

        return ResponseEntity.ok(team);
    }

    @GetMapping("/leave-requests")
    public ResponseEntity<?> getTeamLeaveRequests(@RequestParam(required = false) String status) {
        Employee manager = requireProjectManager();
        if (manager == null) {
            return ResponseEntity.status(403).body(Map.of("error", "Project Manager access required"));
        }

        Set<Long> teamIds = getManagedMembers(manager.getEmpId()).stream()
                .map(Employee::getEmpId)
                .collect(Collectors.toSet());

        List<LeaveRequestDTO> requests = leaveRequestService.getLeaveRequestsForEmployees(teamIds);
        if (status != null && !status.isBlank() && !"all".equalsIgnoreCase(status)) {
            requests = requests.stream()
                    .filter(r -> status.equalsIgnoreCase(r.getStatus()))
                    .toList();
        }
        return ResponseEntity.ok(requests);
    }

    @PatchMapping("/leave-requests/{requestId}/status")
    public ResponseEntity<?> updateTeamLeaveRequestStatus(@PathVariable Long requestId,
                                                           @RequestBody UpdateLeaveRequestStatusDTO statusDTO) {
        Employee manager = requireProjectManager();
        if (manager == null) {
            return ResponseEntity.status(403).body(Map.of("error", "Project Manager access required"));
        }

        Set<Long> teamIds = getManagedMembers(manager.getEmpId()).stream()
                .map(Employee::getEmpId)
                .collect(Collectors.toSet());

        try {
            LeaveRequestDTO existing = leaveRequestService.getLeaveRequestById(requestId);
            if (existing.getEmpId() == null || !teamIds.contains(existing.getEmpId())) {
                return ResponseEntity.status(403).body(Map.of("error", "That request doesn't belong to your team"));
            }
            // No clientId check here - team membership (teamIds.contains above) already proves
            // this request belongs to the manager's own tenant, since Team Structure (Project)
            // membership is itself now client-scoped.
            LeaveRequestDTO updated = leaveRequestService.updateLeaveRequestStatus(requestId, statusDTO, null);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/timesheets")
    public ResponseEntity<?> getTeamTimesheets(@RequestParam(required = false) String status) {
        Employee manager = requireProjectManager();
        if (manager == null) {
            return ResponseEntity.status(403).body(Map.of("error", "Project Manager access required"));
        }

        Set<Long> teamIds = getManagedMembers(manager.getEmpId()).stream()
                .map(Employee::getEmpId)
                .collect(Collectors.toSet());
        if (teamIds.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        List<Timesheet> timesheets = timesheetRepository.findByEmployeeEmpIdInOrderByWorkDateDesc(teamIds);
        List<TimesheetController.TimesheetDTO> result = timesheets.stream()
                .filter(t -> status == null || status.isBlank() || "all".equalsIgnoreCase(status)
                        || status.equalsIgnoreCase(t.getStatus()))
                .map(this::toTimesheetDTO)
                .toList();
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/timesheets/{id}/status")
    public ResponseEntity<?> updateTeamTimesheetStatus(@PathVariable Long id,
                                                        @RequestBody TimesheetController.StatusRequest request) {
        Employee manager = requireProjectManager();
        if (manager == null) {
            return ResponseEntity.status(403).body(Map.of("error", "Project Manager access required"));
        }

        Timesheet timesheet = timesheetRepository.findById(id).orElse(null);
        if (timesheet == null) {
            return ResponseEntity.notFound().build();
        }

        Set<Long> teamIds = getManagedMembers(manager.getEmpId()).stream()
                .map(Employee::getEmpId)
                .collect(Collectors.toSet());
        if (timesheet.getEmployee() == null || !teamIds.contains(timesheet.getEmployee().getEmpId())) {
            return ResponseEntity.status(403).body(Map.of("error", "That timesheet doesn't belong to your team"));
        }
        if (request.status == null || request.status.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "status is required"));
        }

        timesheet.setStatus(request.status.trim());
        Timesheet saved = timesheetRepository.save(timesheet);
        return ResponseEntity.ok(toTimesheetDTO(saved));
    }

    // Bundles this manager's team's not-yet-reported timesheet entries for the given week into a
    // single WeeklyReport sent to HR. Individual daily entries stay approved/reviewed by the PM
    // as before - this is purely the once-a-week rollup HR actually sees.
    @PostMapping("/weekly-reports")
    public ResponseEntity<?> submitWeeklyReport(@RequestBody WeeklyReportRequest request) {
        Employee manager = requireProjectManager();
        if (manager == null) {
            return ResponseEntity.status(403).body(Map.of("error", "Project Manager access required"));
        }
        if (request.weekStartDate == null || request.weekEndDate == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "weekStartDate and weekEndDate are required"));
        }

        LocalDate start;
        LocalDate end;
        try {
            start = LocalDate.parse(request.weekStartDate);
            end = LocalDate.parse(request.weekEndDate);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid week date"));
        }
        if (end.isBefore(start)) {
            return ResponseEntity.badRequest().body(Map.of("error", "weekEndDate must not be before weekStartDate"));
        }

        Set<Long> teamIds = getManagedMembers(manager.getEmpId()).stream()
                .map(Employee::getEmpId)
                .collect(Collectors.toSet());
        if (teamIds.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "You have no team members to report on"));
        }

        List<Timesheet> entries = timesheetRepository
                .findByEmployeeEmpIdInAndWorkDateBetweenAndWeeklyReportIsNullOrderByWorkDateAsc(teamIds, start, end);
        if (entries.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error",
                    "No unreported timesheet entries from your team for that week"));
        }

        WeeklyReport report = new WeeklyReport();
        report.setManager(manager);
        report.setWeekStartDate(start);
        report.setWeekEndDate(end);
        report.setNotes(request.notes);
        report.setSubmittedAt(LocalDate.now());
        WeeklyReport savedReport = weeklyReportRepository.save(report);

        for (Timesheet entry : entries) {
            entry.setWeeklyReport(savedReport);
            timesheetRepository.save(entry);
        }

        return ResponseEntity.ok(toWeeklyReportDTO(savedReport, entries));
    }

    @GetMapping("/weekly-reports")
    public ResponseEntity<?> getMyWeeklyReports() {
        Employee manager = requireProjectManager();
        if (manager == null) {
            return ResponseEntity.status(403).body(Map.of("error", "Project Manager access required"));
        }
        List<WeeklyReportDTO> reports = weeklyReportRepository
                .findByManagerEmpIdOrderByWeekStartDateDesc(manager.getEmpId()).stream()
                .map(report -> toWeeklyReportDTO(report,
                        timesheetRepository.findByWeeklyReportIdOrderByWorkDateAsc(report.getId())))
                .toList();
        return ResponseEntity.ok(reports);
    }

    // A PM's monthly 1-5 rating + comments for one team member. Resubmitting for the same
    // employee+month updates the existing report - see PerformanceReportService.submitReport.
    @PostMapping("/performance-reports")
    public ResponseEntity<?> submitPerformanceReport(@RequestBody SubmitPerformanceReportDTO dto) {
        Employee manager = requireProjectManager();
        if (manager == null) {
            return ResponseEntity.status(403).body(Map.of("error", "Project Manager access required"));
        }

        Set<Long> teamIds = getManagedMembers(manager.getEmpId()).stream()
                .map(Employee::getEmpId)
                .collect(Collectors.toSet());

        try {
            PerformanceReportDTO saved = performanceReportService.submitReport(manager, teamIds, dto);
            return ResponseEntity.ok(saved);
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/performance-reports")
    public ResponseEntity<?> getMyPerformanceReports() {
        Employee manager = requireProjectManager();
        if (manager == null) {
            return ResponseEntity.status(403).body(Map.of("error", "Project Manager access required"));
        }
        return ResponseEntity.ok(performanceReportService.getReportsForManager(manager.getEmpId()));
    }

    private WeeklyReportDTO toWeeklyReportDTO(WeeklyReport report, List<Timesheet> entries) {
        WeeklyReportDTO dto = new WeeklyReportDTO();
        dto.id = report.getId();
        dto.managerName = fullName(report.getManager());
        dto.weekStartDate = report.getWeekStartDate() != null ? report.getWeekStartDate().toString() : null;
        dto.weekEndDate = report.getWeekEndDate() != null ? report.getWeekEndDate().toString() : null;
        dto.notes = report.getNotes();
        dto.submittedAt = report.getSubmittedAt() != null ? report.getSubmittedAt().toString() : null;
        dto.entries = entries.stream().map(this::toTimesheetDTO).toList();
        return dto;
    }

    private TimesheetController.TimesheetDTO toTimesheetDTO(Timesheet timesheet) {
        TimesheetController.TimesheetDTO dto = new TimesheetController.TimesheetDTO();
        dto.id = timesheet.getId();
        Employee employee = timesheet.getEmployee();
        if (employee != null) {
            dto.empId = employee.getEmpId();
            dto.employeeName = fullName(employee);
        }
        dto.workDate = timesheet.getWorkDate() != null ? timesheet.getWorkDate().toString() : null;
        dto.description = timesheet.getDescription();
        dto.hoursWorked = timesheet.getHoursWorked();
        dto.status = timesheet.getStatus();
        dto.submittedTo = timesheet.getSubmittedTo();
        dto.createdAt = timesheet.getCreatedAt() != null ? timesheet.getCreatedAt().toString() : null;
        return dto;
    }

    // Verifies the caller - identified from the verified JWT claim, never a client-supplied id -
    // currently manages at least one active (non-Completed) project, and returns their Employee
    // record (null otherwise). Deliberately checks actual active project ownership rather than
    // JobDetails.positionLevel: that field only ever gets elevated to PROJECT_MANAGER, never
    // downgraded (see ProjectController.elevatePositionLevel), so someone who was assigned as a
    // PM once - even briefly, even by mistake - would otherwise keep "My Team" access forever,
    // including team members and bench employees who never actually manage anything.
    private Employee requireProjectManager() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof AuthenticatedUser authenticatedUser)) {
            return null;
        }
        Employee employee = employeeRepository.findById(authenticatedUser.empId()).orElse(null);
        if (employee == null) {
            return null;
        }
        boolean managesActiveProject = projectRepository.findByProjectManagerEmpId(employee.getEmpId()).stream()
                .anyMatch(p -> !"Completed".equalsIgnoreCase(p.getStatus()));
        return managesActiveProject ? employee : null;
    }

    // Every employee with an OPEN membership in any project this manager currently manages.
    private List<Employee> getManagedMembers(Long managerEmpId) {
        Map<Long, Employee> members = new LinkedHashMap<>();
        for (Project project : projectRepository.findByProjectManagerEmpId(managerEmpId)) {
            for (ProjectMembership membership : membershipRepository.findByProjectId(project.getId())) {
                if (membership.getEndDate() == null) {
                    members.putIfAbsent(membership.getEmployee().getEmpId(), membership.getEmployee());
                }
            }
        }
        return new ArrayList<>(members.values());
    }

    private String fullName(Employee employee) {
        return String.format("%s %s",
                employee.getFirstName() == null ? "" : employee.getFirstName(),
                employee.getLastName() == null ? "" : employee.getLastName()).trim();
    }

    public static class TeamMemberDTO {
        public Long empId;
        public String name;
        public String designation;
        public String department;
        public String attendanceStatus;
    }

    public static class WeeklyReportRequest {
        public String weekStartDate;
        public String weekEndDate;
        public String notes;
    }

    public static class WeeklyReportDTO {
        public Long id;
        public String managerName;
        public String weekStartDate;
        public String weekEndDate;
        public String notes;
        public String submittedAt;
        public List<TimesheetController.TimesheetDTO> entries;
    }
}
