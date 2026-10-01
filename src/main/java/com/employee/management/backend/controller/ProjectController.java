package com.employee.management.backend.controller;

import com.employee.management.backend.Entity.Employee;
import com.employee.management.backend.Entity.JobDetails;
import com.employee.management.backend.Entity.Project;
import com.employee.management.backend.Entity.ProjectHistory;
import com.employee.management.backend.Entity.ProjectMembership;
import com.employee.management.backend.repository.EmployeeRepository;
import com.employee.management.backend.repository.ProjectMembershipRepository;
import com.employee.management.backend.repository.ProjectRepository;
import com.employee.management.backend.security.SecurityUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin-facing hierarchy: Project -> Project Manager -> Team Leads -> their reports,
 * plus a bench roster (employees with no active project membership at all).
 */
@RestController
@RequestMapping("/api/projects")
@PreAuthorize("hasRole('ADMIN')")
public class ProjectController {

    private static final List<String> POSITION_LEVEL_RANK = List.of("EMPLOYEE", "TEAM_LEAD", "PROJECT_MANAGER");

    private final ProjectRepository projectRepository;
    private final ProjectMembershipRepository membershipRepository;
    private final EmployeeRepository employeeRepository;

    public ProjectController(ProjectRepository projectRepository, ProjectMembershipRepository membershipRepository,
                              EmployeeRepository employeeRepository) {
        this.projectRepository = projectRepository;
        this.membershipRepository = membershipRepository;
        this.employeeRepository = employeeRepository;
    }

    @GetMapping
    public List<ProjectSummaryDTO> getAllProjects() {
        return projectRepository.findAllByClientIdOrderByNameAsc(SecurityUtils.currentClientId()).stream()
                .map(project -> toSummaryDTO(project, membershipRepository.findByProjectId(project.getId()).size()))
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getProject(@PathVariable Long id) {
        Project project = projectRepository.findByIdAndClientId(id, SecurityUtils.currentClientId()).orElse(null);
        if (project == null) {
            return ResponseEntity.notFound().build();
        }

        ProjectDetailDTO dto = new ProjectDetailDTO();
        dto.id = project.getId();
        dto.name = project.getName();
        dto.description = project.getDescription();
        dto.status = project.getStatus();
        dto.startDate = project.getStartDate();
        dto.endDate = project.getEndDate();
        dto.projectManager = toEmployeeSummary(project.getProjectManager());
        dto.members = membershipRepository.findByProjectId(id).stream().map(this::toMemberDTO).toList();
        return ResponseEntity.ok(dto);
    }

    @PostMapping
    public ResponseEntity<?> createProject(@RequestBody ProjectRequest request) {
        if (request.name == null || request.name.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Project name is required"));
        }
        Long clientId = SecurityUtils.currentClientId();

        Project project = new Project();
        project.setClientId(clientId);
        project.setName(request.name.trim());
        project.setDescription(request.description);
        project.setStatus(request.status == null || request.status.isBlank() ? "Active" : request.status);
        project.setStartDate(request.startDate);
        project.setEndDate(request.endDate);

        if (request.projectManagerId != null) {
            Employee pm = employeeRepository.findById(request.projectManagerId).orElse(null);
            if (pm == null || pm.getClient() == null || !pm.getClient().getId().equals(clientId)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Project manager not found"));
            }
            project.setProjectManager(pm);
            elevatePositionLevel(pm, "PROJECT_MANAGER");
        }

        Project saved = projectRepository.save(project);
        return ResponseEntity.ok(toSummaryDTO(saved, 0));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateProject(@PathVariable Long id, @RequestBody ProjectRequest request) {
        Long clientId = SecurityUtils.currentClientId();
        Project project = projectRepository.findByIdAndClientId(id, clientId).orElse(null);
        if (project == null) {
            return ResponseEntity.notFound().build();
        }

        if (request.name != null && !request.name.trim().isEmpty()) {
            project.setName(request.name.trim());
        }
        project.setDescription(request.description);
        if (request.status != null && !request.status.isBlank()) {
            project.setStatus(request.status);
        }
        project.setStartDate(request.startDate);
        project.setEndDate(request.endDate);

        if (request.projectManagerId != null) {
            Employee pm = employeeRepository.findById(request.projectManagerId).orElse(null);
            if (pm == null || pm.getClient() == null || !pm.getClient().getId().equals(clientId)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Project manager not found"));
            }
            project.setProjectManager(pm);
            elevatePositionLevel(pm, "PROJECT_MANAGER");
        } else {
            project.setProjectManager(null);
        }

        Project saved = projectRepository.save(project);
        return ResponseEntity.ok(toSummaryDTO(saved, membershipRepository.findByProjectId(id).size()));
    }

    // Separate from the general PUT above on purpose: PUT expects the full project payload and
    // clears the project manager if projectManagerId is omitted, which would silently wipe the PM
    // on a "just mark this Completed" action. This only ever touches status.
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateProjectStatus(@PathVariable Long id, @RequestBody StatusRequest request) {
        Project project = projectRepository.findByIdAndClientId(id, SecurityUtils.currentClientId()).orElse(null);
        if (project == null) {
            return ResponseEntity.notFound().build();
        }
        if (request.status == null || request.status.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "status is required"));
        }

        String newStatus = request.status.trim();
        boolean wasCompleted = "Completed".equalsIgnoreCase(project.getStatus());
        boolean becomingCompleted = "Completed".equalsIgnoreCase(newStatus) && !wasCompleted;
        boolean becomingReactivated = wasCompleted && !"Completed".equalsIgnoreCase(newStatus);

        project.setStatus(newStatus);
        Project saved = projectRepository.save(project);

        if (becomingCompleted) {
            cascadeProjectCompletion(saved);
        } else if (becomingReactivated) {
            cascadeProjectReactivation(saved);
        }

        return ResponseEntity.ok(toSummaryDTO(saved, membershipRepository.findByProjectId(id).size()));
    }

    // When a project completes, everyone's assignment to it ends that day. Anyone left with no
    // other open assignment - and no other still-active project they manage - rolls onto the
    // bench, with today recorded as their end date (both on the membership row and, so the
    // Employee Profile's own Project History reflects it too, on the matching legacy record).
    private void cascadeProjectCompletion(Project project) {
        String today = LocalDate.now().toString();

        for (ProjectMembership m : membershipRepository.findByProjectId(project.getId())) {
            if (m.getEndDate() != null) {
                continue; // this person had already left before the project completed
            }
            m.setEndDate(today);
            membershipRepository.save(m);
            benchIfNoOtherActiveWork(m.getEmployee(), project.getId(), today);
        }

        if (project.getProjectManager() != null) {
            benchIfNoOtherActiveWork(project.getProjectManager(), project.getId(), today);
        }
    }

    private void benchIfNoOtherActiveWork(Employee employee, Long completedProjectId, String today) {
        boolean openElsewhere = membershipRepository.findByEmployeeEmpId(employee.getEmpId()).stream()
                .anyMatch(m -> m.getEndDate() == null);
        boolean managesOtherActiveProject = projectRepository.findByProjectManagerEmpId(employee.getEmpId()).stream()
                .anyMatch(p -> !p.getId().equals(completedProjectId) && !"Completed".equalsIgnoreCase(p.getStatus()));
        if (!openElsewhere && !managesOtherActiveProject) {
            moveEmployeeToBench(employee, today);
        }
    }

    // Mirror of cascadeProjectCompletion, run when a Completed project flips back to Active.
    // Only pulls back people who are still sitting idle on the bench today - if someone moved on
    // to different work after this project completed, reactivating it must not silently yank
    // them off whatever they're doing now, so their ended membership row here is left alone.
    private void cascadeProjectReactivation(Project project) {
        String today = LocalDate.now().toString();
        String pmName = fullName(project.getProjectManager());

        for (ProjectMembership m : membershipRepository.findByProjectId(project.getId())) {
            if (m.getEndDate() == null) {
                continue; // never left in the first place
            }
            Employee employee = m.getEmployee();
            JobDetails jobDetails = employee.getJobDetails();
            if (jobDetails == null || !"Bench".equalsIgnoreCase(jobDetails.getWorkStatus())) {
                continue; // busy elsewhere now - don't pull them back onto this project
            }
            m.setEndDate(null);
            m.setStartDate(today);
            membershipRepository.save(m);
            restoreEmployeeToProject(employee, project.getName(), pmName, today);
        }

        Employee pm = project.getProjectManager();
        if (pm != null && pm.getJobDetails() != null && "Bench".equalsIgnoreCase(pm.getJobDetails().getWorkStatus())) {
            restoreEmployeeToProject(pm, project.getName(), null, today);
        }
    }

    private void restoreEmployeeToProject(Employee employee, String projectName, String projectManagerName, String today) {
        JobDetails jobDetails = employee.getJobDetails();
        if (jobDetails == null) {
            return;
        }

        jobDetails.setWorkStatus("Project");
        jobDetails.setCurrentProjectName(projectName);
        jobDetails.setCurrentProjectManager(projectManagerName);
        jobDetails.setCurrentProjectStartDate(today);

        ProjectHistory entry = new ProjectHistory();
        entry.setEmployee(employee);
        entry.setProjectName(projectName);
        entry.setProjectManager(projectManagerName);
        entry.setStartDate(today);
        employee.getProjectHistory().add(entry);

        employeeRepository.save(employee);
    }

    private String fullName(Employee employee) {
        if (employee == null) {
            return null;
        }
        return String.format("%s %s",
                employee.getFirstName() == null ? "" : employee.getFirstName(),
                employee.getLastName() == null ? "" : employee.getLastName()).trim();
    }

    private void moveEmployeeToBench(Employee employee, String today) {
        JobDetails jobDetails = employee.getJobDetails();
        if (jobDetails == null) {
            return;
        }

        String currentProjectName = jobDetails.getCurrentProjectName();
        if (currentProjectName != null && employee.getProjectHistory() != null) {
            employee.getProjectHistory().stream()
                    .filter(h -> currentProjectName.equalsIgnoreCase(h.getProjectName())
                            && (h.getEndDate() == null || h.getEndDate().isBlank()))
                    .findFirst()
                    .ifPresent(h -> h.setEndDate(today));
        }

        jobDetails.setWorkStatus("Bench");
        jobDetails.setCurrentProjectName(null);
        jobDetails.setCurrentProjectManager(null);
        jobDetails.setCurrentProjectStartDate(null);
        employeeRepository.save(employee);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProject(@PathVariable Long id) {
        if (projectRepository.findByIdAndClientId(id, SecurityUtils.currentClientId()).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        projectRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Project deleted"));
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<?> addMember(@PathVariable Long id, @RequestBody MemberRequest request) {
        Long clientId = SecurityUtils.currentClientId();
        Project project = projectRepository.findByIdAndClientId(id, clientId).orElse(null);
        if (project == null) {
            return ResponseEntity.notFound().build();
        }
        if (request.employeeId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "employeeId is required"));
        }

        Employee employee = employeeRepository.findById(request.employeeId).orElse(null);
        if (employee == null || employee.getClient() == null || !employee.getClient().getId().equals(clientId)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Employee not found"));
        }

        // The (project, employee) pair is unique, so re-adding someone whose earlier stint here
        // already ended (e.g. the project was reactivated) resurrects that same row instead of
        // trying to insert a duplicate.
        ProjectMembership membership = membershipRepository.findByProjectIdAndEmployeeEmpId(id, request.employeeId).orElse(null);
        if (membership != null && membership.getEndDate() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Employee is already on this project"));
        }
        if (membership == null) {
            membership = new ProjectMembership();
            membership.setProject(project);
            membership.setEmployee(employee);
        }
        membership.setEndDate(null);
        membership.setStartDate(request.startDate == null || request.startDate.isBlank()
                ? LocalDate.now().toString() : request.startDate);

        ResponseEntity<?> teamLeadError = applyTeamLead(membership, id, request.teamLeadId, request.employeeId);
        if (teamLeadError != null) {
            return teamLeadError;
        }

        ProjectMembership saved = membershipRepository.save(membership);
        return ResponseEntity.ok(toMemberDTO(saved));
    }

    @PutMapping("/{id}/members/{membershipId}")
    public ResponseEntity<?> updateMember(@PathVariable Long id, @PathVariable Long membershipId,
                                           @RequestBody MemberRequest request) {
        Long clientId = SecurityUtils.currentClientId();
        if (projectRepository.findByIdAndClientId(id, clientId).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        ProjectMembership membership = membershipRepository.findById(membershipId).orElse(null);
        if (membership == null || !membership.getProject().getId().equals(id)) {
            return ResponseEntity.notFound().build();
        }

        ResponseEntity<?> teamLeadError = applyTeamLead(membership, id, request.teamLeadId, membership.getEmployee().getEmpId());
        if (teamLeadError != null) {
            return teamLeadError;
        }

        ProjectMembership saved = membershipRepository.save(membership);
        return ResponseEntity.ok(toMemberDTO(saved));
    }

    @DeleteMapping("/{id}/members/{membershipId}")
    public ResponseEntity<?> removeMember(@PathVariable Long id, @PathVariable Long membershipId) {
        if (projectRepository.findByIdAndClientId(id, SecurityUtils.currentClientId()).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        ProjectMembership membership = membershipRepository.findById(membershipId).orElse(null);
        if (membership == null || !membership.getProject().getId().equals(id)) {
            return ResponseEntity.notFound().build();
        }

        // Anyone who had this person as their team lead falls back to reporting
        // directly to the project manager rather than being left dangling.
        Long removedEmpId = membership.getEmployee().getEmpId();
        membershipRepository.findByProjectId(id).stream()
                .filter(m -> m.getTeamLead() != null && m.getTeamLead().getEmpId().equals(removedEmpId))
                .forEach(m -> {
                    m.setTeamLead(null);
                    membershipRepository.save(m);
                });

        membershipRepository.deleteById(membershipId);
        return ResponseEntity.ok(Map.of("message", "Member removed"));
    }

    @GetMapping("/hierarchy")
    public OrgHierarchyDTO getOrgHierarchy() {
        Long clientId = SecurityUtils.currentClientId();
        List<Project> projects = projectRepository.findAllByClientIdOrderByNameAsc(clientId);

        List<ProjectHierarchyDTO> projectDTOs = projects.stream().map(project -> {
            List<ProjectMembership> memberships = membershipRepository.findByProjectId(project.getId());

            // Team leads for this project = members marked TEAM_LEAD on their job details.
            // Keyed by empId (not Employee identity) since dedup must survive across rows.
            Map<Long, Employee> teamLeadEmployees = new LinkedHashMap<>();
            for (ProjectMembership m : memberships) {
                Employee emp = m.getEmployee();
                JobDetails jd = emp.getJobDetails();
                if (jd != null && "TEAM_LEAD".equalsIgnoreCase(jd.getPositionLevel())) {
                    teamLeadEmployees.putIfAbsent(emp.getEmpId(), emp);
                }
            }

            Map<Long, List<EmployeeSummaryDTO>> membersByTeamLead = new LinkedHashMap<>();
            for (ProjectMembership m : memberships) {
                if (m.getTeamLead() != null) {
                    membersByTeamLead
                            .computeIfAbsent(m.getTeamLead().getEmpId(), k -> new java.util.ArrayList<>())
                            .add(toEmployeeSummary(m.getEmployee()));
                }
            }

            List<TeamLeadGroupDTO> teamLeadGroups = teamLeadEmployees.values().stream()
                    .map(tl -> {
                        TeamLeadGroupDTO group = new TeamLeadGroupDTO();
                        group.teamLead = toEmployeeSummary(tl);
                        group.members = membersByTeamLead.getOrDefault(tl.getEmpId(), List.of());
                        return group;
                    })
                    .toList();

            List<EmployeeSummaryDTO> directReports = memberships.stream()
                    .filter(m -> m.getTeamLead() == null && !teamLeadEmployees.containsKey(m.getEmployee().getEmpId()))
                    .map(m -> toEmployeeSummary(m.getEmployee()))
                    .toList();

            ProjectHierarchyDTO dto = new ProjectHierarchyDTO();
            dto.id = project.getId();
            dto.name = project.getName();
            dto.status = project.getStatus();
            dto.projectManager = toEmployeeSummary(project.getProjectManager());
            dto.teamLeads = teamLeadGroups;
            dto.directReports = directReports;
            return dto;
        }).toList();

        OrgHierarchyDTO response = new OrgHierarchyDTO();
        response.projects = projectDTOs;
        response.bench = membershipRepository.findEmployeesNotOnAnyProject(clientId).stream()
                .map(this::toEmployeeSummary)
                .sorted(Comparator.comparing(e -> e.name == null ? "" : e.name))
                .toList();
        return response;
    }

    @GetMapping("/eligible-managers")
    public List<EmployeeSummaryDTO> getEligibleManagers() {
        return employeeRepository.findByClientId(SecurityUtils.currentClientId(), Pageable.unpaged()).stream()
                .filter(emp -> emp.getJobDetails() != null
                        && "PROJECT_MANAGER".equalsIgnoreCase(emp.getJobDetails().getPositionLevel()))
                .map(this::toEmployeeSummary)
                .sorted(Comparator.comparing(e -> e.name == null ? "" : e.name))
                .toList();
    }

    @GetMapping("/eligible-team-leads")
    public List<EmployeeSummaryDTO> getEligibleTeamLeads() {
        return employeeRepository.findByClientId(SecurityUtils.currentClientId(), Pageable.unpaged()).stream()
                .filter(emp -> emp.getJobDetails() != null
                        && "TEAM_LEAD".equalsIgnoreCase(emp.getJobDetails().getPositionLevel()))
                .map(this::toEmployeeSummary)
                .sorted(Comparator.comparing(e -> e.name == null ? "" : e.name))
                .toList();
    }

    private ResponseEntity<?> applyTeamLead(ProjectMembership membership, Long projectId, Long teamLeadId, Long employeeId) {
        if (teamLeadId == null) {
            membership.setTeamLead(null);
            return null;
        }
        if (teamLeadId.equals(employeeId)) {
            return ResponseEntity.badRequest().body(Map.of("error", "An employee cannot be their own team lead"));
        }
        ProjectMembership teamLeadMembership = membershipRepository
                .findByProjectIdAndEmployeeEmpId(projectId, teamLeadId).orElse(null);
        if (teamLeadMembership == null || teamLeadMembership.getEndDate() != null) {
            return ResponseEntity.badRequest().body(Map.of("error",
                    "The chosen team lead must already be an active member of this project"));
        }
        // The hierarchy view groups a project's team leads by positionLevel == TEAM_LEAD, so
        // whoever gets assigned as someone's lead here must actually carry that level - otherwise
        // their reports would have a non-null teamLead that never surfaces as a group header and
        // silently disappear from the rendered tree.
        elevatePositionLevel(teamLeadMembership.getEmployee(), "TEAM_LEAD");
        membership.setTeamLead(teamLeadMembership.getEmployee());
        return null;
    }

    // Never downgrades - a Project Manager assigned as a plain member elsewhere stays a PM.
    private void elevatePositionLevel(Employee employee, String requiredLevel) {
        JobDetails jobDetails = employee.getJobDetails();
        if (jobDetails == null) {
            return;
        }
        int currentRank = POSITION_LEVEL_RANK.indexOf(jobDetails.getPositionLevel());
        int requiredRank = POSITION_LEVEL_RANK.indexOf(requiredLevel);
        if (requiredRank > currentRank) {
            jobDetails.setPositionLevel(requiredLevel);
            employeeRepository.save(employee);
        }
    }

    private EmployeeSummaryDTO toEmployeeSummary(Employee employee) {
        if (employee == null) {
            return null;
        }
        EmployeeSummaryDTO dto = new EmployeeSummaryDTO();
        dto.empId = employee.getEmpId();
        dto.name = fullName(employee);
        JobDetails jd = employee.getJobDetails();
        if (jd != null) {
            dto.designation = jd.getDesignation();
            dto.department = jd.getDepartment();
            dto.positionLevel = jd.getPositionLevel();
        }
        return dto;
    }

    private ProjectSummaryDTO toSummaryDTO(Project project, int memberCount) {
        ProjectSummaryDTO dto = new ProjectSummaryDTO();
        dto.id = project.getId();
        dto.name = project.getName();
        dto.description = project.getDescription();
        dto.status = project.getStatus();
        dto.startDate = project.getStartDate();
        dto.endDate = project.getEndDate();
        dto.projectManager = toEmployeeSummary(project.getProjectManager());
        dto.memberCount = memberCount;
        return dto;
    }

    private ProjectMemberDTO toMemberDTO(ProjectMembership membership) {
        ProjectMemberDTO dto = new ProjectMemberDTO();
        dto.membershipId = membership.getId();
        dto.employee = toEmployeeSummary(membership.getEmployee());
        dto.teamLead = toEmployeeSummary(membership.getTeamLead());
        dto.startDate = membership.getStartDate();
        dto.endDate = membership.getEndDate();
        return dto;
    }

    public static class EmployeeSummaryDTO {
        public Long empId;
        public String name;
        public String designation;
        public String department;
        public String positionLevel;
    }

    public static class ProjectSummaryDTO {
        public Long id;
        public String name;
        public String description;
        public String status;
        public String startDate;
        public String endDate;
        public EmployeeSummaryDTO projectManager;
        public int memberCount;
    }

    public static class ProjectMemberDTO {
        public Long membershipId;
        public EmployeeSummaryDTO employee;
        public EmployeeSummaryDTO teamLead;
        public String startDate;
        public String endDate;
    }

    public static class ProjectDetailDTO {
        public Long id;
        public String name;
        public String description;
        public String status;
        public String startDate;
        public String endDate;
        public EmployeeSummaryDTO projectManager;
        public List<ProjectMemberDTO> members;
    }

    public static class TeamLeadGroupDTO {
        public EmployeeSummaryDTO teamLead;
        public List<EmployeeSummaryDTO> members;
    }

    public static class ProjectHierarchyDTO {
        public Long id;
        public String name;
        public String status;
        public EmployeeSummaryDTO projectManager;
        public List<TeamLeadGroupDTO> teamLeads;
        public List<EmployeeSummaryDTO> directReports;
    }

    public static class OrgHierarchyDTO {
        public List<ProjectHierarchyDTO> projects;
        public List<EmployeeSummaryDTO> bench;
    }

    public static class ProjectRequest {
        public String name;
        public String description;
        public String status;
        public String startDate;
        public String endDate;
        public Long projectManagerId;
    }

    public static class MemberRequest {
        public Long employeeId;
        public Long teamLeadId;
        public String startDate;
    }

    public static class StatusRequest {
        public String status;
    }
}
