package com.employee.management.backend.controller;

import com.employee.management.backend.Entity.Employee;
import com.employee.management.backend.security.SecurityUtils;
import com.employee.management.backend.service.EmployeeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/api/reports/employees", "/api/employees/reports"})
public class ReportsController {
    private final EmployeeService employeeService;

    public ReportsController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping
    public Page<ReportEmployeeDTO> getEmployeeReport(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String employeeType) {
        int normalizedPage = Math.max(page, 0);
        int normalizedSize = Math.max(size, 1);
        String normalizedSearch = (search == null || search.trim().isEmpty()) ? null : search.trim();
        String normalizedDepartment = normalizeFilterValue(department);
        String normalizedStatus = normalizeFilterValue(status);
        String normalizedEmployeeType = normalizeFilterValue(employeeType);

        Page<Employee> employees = employeeService.searchEmployees(
                SecurityUtils.currentClientId(),
                normalizedSearch,
                normalizedDepartment,
                normalizedStatus,
                normalizedEmployeeType,
                PageRequest.of(normalizedPage, normalizedSize)
        );

        List<ReportEmployeeDTO> content = employees.getContent().stream()
                .map(this::toReportDto)
                .toList();

        return new PageImpl<>(content, employees.getPageable(), employees.getTotalElements());
    }

    private ReportEmployeeDTO toReportDto(Employee employee) {
        ReportEmployeeDTO dto = new ReportEmployeeDTO();
        dto.setEmpId(employee.getEmpId());
        dto.setFullName(String.format("%s %s", employee.getFirstName(), employee.getLastName()).trim());
        dto.setEmail(employee.getEmail());
        dto.setDepartment(employee.getJobDetails() != null ? employee.getJobDetails().getDepartment() : null);
        dto.setStatus(employee.getJobDetails() != null ? employee.getJobDetails().getEmployeeStatus() : null);
        dto.setDesignation(employee.getJobDetails() != null ? employee.getJobDetails().getDesignation() : null);
        return dto;
    }

    private String normalizeFilterValue(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty() || "all".equalsIgnoreCase(trimmed)) {
            return null;
        }
        return trimmed;
    }

    public static class ReportEmployeeDTO {
        private Long empId;
        private String fullName;
        private String email;
        private String department;
        private String status;
        private String designation;

        public Long getEmpId() {
            return empId;
        }

        public void setEmpId(Long empId) {
            this.empId = empId;
        }

        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getDepartment() {
            return department;
        }

        public void setDepartment(String department) {
            this.department = department;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getDesignation() {
            return designation;
        }

        public void setDesignation(String designation) {
            this.designation = designation;
        }
    }
}
