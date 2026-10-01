package com.employee.management.backend.service;

import com.employee.management.backend.Entity.Employee;
import com.employee.management.backend.Entity.PerformanceReport;
import com.employee.management.backend.dto.PerformanceReportDTO;
import com.employee.management.backend.dto.SubmitPerformanceReportDTO;
import com.employee.management.backend.exception.ResourceNotFoundException;
import com.employee.management.backend.repository.EmployeeRepository;
import com.employee.management.backend.repository.PerformanceReportRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class PerformanceReportService {

    private static final DateTimeFormatter DISPLAY_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    private final PerformanceReportRepository performanceReportRepository;
    private final EmployeeRepository employeeRepository;

    public PerformanceReportService(PerformanceReportRepository performanceReportRepository,
                                     EmployeeRepository employeeRepository) {
        this.performanceReportRepository = performanceReportRepository;
        this.employeeRepository = employeeRepository;
    }

    public PerformanceReportDTO submitReport(Employee manager, Set<Long> managedTeamIds, SubmitPerformanceReportDTO dto) {
        if (dto.getEmpId() == null || !managedTeamIds.contains(dto.getEmpId())) {
            throw new RuntimeException("That employee is not on your team");
        }
        if (dto.getRating() == null || dto.getRating() < 1 || dto.getRating() > 5) {
            throw new RuntimeException("Rating must be a whole number from 1 to 5");
        }
        if (dto.getMonth() == null || dto.getMonth() < 1 || dto.getMonth() > 12) {
            throw new RuntimeException("Month must be between 1 and 12");
        }
        int currentYear = Year.now().getValue();
        if (dto.getYear() == null || dto.getYear() < 2000 || dto.getYear() > currentYear + 1) {
            throw new RuntimeException("Year is out of range");
        }

        Employee employee = employeeRepository.findById(dto.getEmpId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "empId", dto.getEmpId()));

        PerformanceReport report = performanceReportRepository
                .findByEmployeeEmpIdAndMonthAndYear(dto.getEmpId(), dto.getMonth(), dto.getYear())
                .orElseGet(PerformanceReport::new);

        boolean isNew = report.getId() == null;
        report.setEmployee(employee);
        report.setManager(manager);
        report.setMonth(dto.getMonth());
        report.setYear(dto.getYear());
        report.setRating(dto.getRating());
        report.setComments(dto.getComments() != null ? dto.getComments().trim() : null);
        LocalDateTime now = LocalDateTime.now();
        if (isNew) {
            report.setSubmittedAt(now);
        }
        report.setUpdatedAt(now);

        return convertToDTO(performanceReportRepository.save(report));
    }

    public List<PerformanceReportDTO> getReportsForManager(Long managerEmpId) {
        return performanceReportRepository.findByManagerEmpIdOrderByYearDescMonthDesc(managerEmpId)
                .stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public Page<PerformanceReportDTO> getReportsForClientPage(Long clientId, Integer month, String employeeName, Pageable pageable) {
        return performanceReportRepository.filterForClient(clientId, month, employeeName, pageable)
                .map(this::convertToDTO);
    }

    private String fullName(Employee employee) {
        return String.format("%s %s",
                employee.getFirstName() == null ? "" : employee.getFirstName(),
                employee.getLastName() == null ? "" : employee.getLastName()).trim();
    }

    private PerformanceReportDTO convertToDTO(PerformanceReport report) {
        return new PerformanceReportDTO(
                report.getId(),
                report.getEmployee().getEmpId(),
                fullName(report.getEmployee()),
                report.getManager().getEmpId(),
                fullName(report.getManager()),
                report.getMonth(),
                report.getYear(),
                report.getRating(),
                report.getComments(),
                report.getSubmittedAt().format(DISPLAY_DATE_FORMATTER),
                report.getUpdatedAt().format(DISPLAY_DATE_FORMATTER)
        );
    }
}
