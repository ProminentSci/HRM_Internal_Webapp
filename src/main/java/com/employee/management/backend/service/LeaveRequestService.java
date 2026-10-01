package com.employee.management.backend.service;

import com.employee.management.backend.Entity.Employee;
import com.employee.management.backend.Entity.LeaveBalance;
import com.employee.management.backend.Entity.LeaveHistory;
import com.employee.management.backend.Entity.LeaveRequest;
import com.employee.management.backend.dto.CreateLeaveRequestDTO;
import com.employee.management.backend.dto.LeaveRequestDTO;
import com.employee.management.backend.dto.UpdateLeaveRequestStatusDTO;
import com.employee.management.backend.exception.ResourceNotFoundException;
import com.employee.management.backend.repository.EmployeeRepository;
import com.employee.management.backend.repository.LeaveBalanceRepository;
import com.employee.management.backend.repository.LeaveHistoryRepository;
import com.employee.management.backend.repository.LeaveRequestRepository;
import com.employee.management.backend.repository.HolidayRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class LeaveRequestService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final LeaveHistoryRepository leaveHistoryRepository;
    private final HolidayRepository holidayRepository;
    private final EmailService emailService;
    private final String mailFrom;
    private final String adminNotificationEmail;
    private final String adminLeaveDashboardUrl;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_DATE;
    private static final 
    DateTimeFormatter DISPLAY_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy");

    public LeaveRequestService(LeaveRequestRepository leaveRequestRepository,
                               EmployeeRepository employeeRepository,
                               LeaveBalanceRepository leaveBalanceRepository,
                               LeaveHistoryRepository leaveHistoryRepository,
                               HolidayRepository holidayRepository,
                               EmailService emailService,
                               @Value("${app.mail.from}") String mailFrom,
                               @Value("${app.admin.notification-email}") String adminNotificationEmail,
                               @Value("${app.frontend-url}") String frontendUrl,
                               @Value("${app.admin.leave-dashboard-path}") String adminLeaveDashboardPath) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.employeeRepository = employeeRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.leaveHistoryRepository = leaveHistoryRepository;
        this.holidayRepository = holidayRepository;
        this.emailService = emailService;
        this.mailFrom = mailFrom;
        this.adminNotificationEmail = adminNotificationEmail;
        this.adminLeaveDashboardUrl = frontendUrl + adminLeaveDashboardPath;
    }

    public LeaveRequestDTO createLeaveRequest(CreateLeaveRequestDTO requestDTO) {
    	System.out.println(requestDTO.getEmpId());
    	System.out.println(employeeRepository.findById(requestDTO.getEmpId()));
        Employee employee = employeeRepository.findById(requestDTO.getEmpId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "empId", requestDTO.getEmpId()));

        // Parse dates
        LocalDate fromDate = LocalDate.parse(requestDTO.getFromDate(), DATE_FORMATTER);
        LocalDate toDate = LocalDate.parse(requestDTO.getToDate(), DATE_FORMATTER);

        // Calculate calendar days (inclusive) and working days (exclude weekends and holidays)
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(fromDate, toDate) + 1;
        if (daysBetween <= 0) {
            throw new RuntimeException("Invalid date range");
        }

        int workingDays = calculateWorkingDays(fromDate, toDate, employee.getClient() == null ? null : employee.getClient().getId());

        boolean hasOverlappingRequest = leaveRequestRepository.findByEmployeeEmpIdOrderByCreatedAtDesc(requestDTO.getEmpId())
                .stream()
                .filter(existing -> "Pending".equalsIgnoreCase(existing.getStatus()) || "Approved".equalsIgnoreCase(existing.getStatus()))
                .anyMatch(existing -> !existing.getToDate().isBefore(fromDate) && !existing.getFromDate().isAfter(toDate));

        if (hasOverlappingRequest) {
            throw new RuntimeException("A pending or approved leave request already exists for the selected dates");
        }

        // Use provided year or default to current year
        Integer year = requestDTO.getYear();
        if (year == null) {
            year = java.time.LocalDate.now().getYear();
        }

        // Check leave balance against working days
        LeaveBalance leaveBalance = leaveBalanceRepository
            .findByEmployeeEmpIdAndLeaveType(requestDTO.getEmpId(), requestDTO.getLeaveType())
            .orElseThrow(() -> new RuntimeException("Leave balance not found for this leave type"));

        if (leaveBalance.getBalance() < workingDays) {
            throw new RuntimeException("Insufficient leave balance. Available: " + leaveBalance.getBalance() +
                " days, Requested: " + workingDays + " working days");
        }

        // Create leave request
        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setEmployee(employee);
        leaveRequest.setLeaveType(requestDTO.getLeaveType());
        leaveRequest.setDays(workingDays);
        leaveRequest.setYear(year);
        leaveRequest.setFromDate(fromDate);
        leaveRequest.setToDate(toDate);
        leaveRequest.setCreatedAt(LocalDate.now());
        leaveRequest.setReason(requestDTO.getReason());
        leaveRequest.setStatus("Pending");

        LeaveRequest savedRequest = leaveRequestRepository.save(leaveRequest);
        sendAdminLeaveNotification(employee, savedRequest);
        return convertToDTO(savedRequest);
    }

    private void sendAdminLeaveNotification(Employee employee, LeaveRequest leaveRequest) {
        if (adminNotificationEmail == null || adminNotificationEmail.trim().isEmpty()) {
            return;
        }

        String employeeName = String.format("%s %s",
                employee.getFirstName() == null ? "" : employee.getFirstName(),
                employee.getLastName() == null ? "" : employee.getLastName()).trim();

        String replyTo = employee.getEmail() != null && !employee.getEmail().trim().isEmpty()
                ? employee.getEmail() : null;
        emailService.sendHtmlEmail(adminNotificationEmail, mailFrom, replyTo,
                "New Leave Request from " + employeeName,
                buildAdminNotificationBody(employeeName, employee, leaveRequest));
    }

    private String buildAdminNotificationBody(String employeeName, Employee employee, LeaveRequest leaveRequest) {
        return "<p>Hello,</p>"
                + "<p>A new leave request has been submitted and is awaiting your review:</p>"
                + "<ul>"
                + "<li><strong>Employee Name:</strong> " + employeeName + "</li>"
                + "<li><strong>Employee ID:</strong> " + employee.getEmpId() + "</li>"
                + "<li><strong>Leave Type:</strong> " + leaveRequest.getLeaveType() + "</li>"
                + "<li><strong>From Date:</strong> " + leaveRequest.getFromDate().format(DISPLAY_DATE_FORMATTER) + "</li>"
                + "<li><strong>To Date:</strong> " + leaveRequest.getToDate().format(DISPLAY_DATE_FORMATTER) + "</li>"
                + "<li><strong>Reason:</strong> " + leaveRequest.getReason() + "</li>"
                + "</ul>"
                + "<p><a href=\"" + adminLeaveDashboardUrl + "\">Review this request on the Leave Management dashboard</a></p>";
    }

    private String normalizeStatus(String status) {
        if (status == null) {
            throw new RuntimeException("Status is required");
        }
        String trimmed = status.trim();
        if ("pending".equalsIgnoreCase(trimmed)) return "Pending";
        if ("approved".equalsIgnoreCase(trimmed)) return "Approved";
        if ("rejected".equalsIgnoreCase(trimmed)) return "Rejected";
        if ("cancelled".equalsIgnoreCase(trimmed) || "canceled".equalsIgnoreCase(trimmed)) return "Cancelled";
        throw new RuntimeException("Invalid status: " + status);
    }

    private int calculateWorkingDays(LocalDate start, LocalDate end, Long clientId) {
        if (start.isAfter(end)) return 0;
        // fetch holidays in range, scoped to the leave request's own employee's client - never
        // the caller's client, since an admin approving a request acts on someone else's company.
        List<com.employee.management.backend.Entity.Holiday> holidays = clientId == null
                ? List.of()
                : holidayRepository.findByClientIdAndDateBetweenOrderByDateAsc(clientId, start, end);
        java.util.Set<LocalDate> holidayDates = holidays.stream().map(com.employee.management.backend.Entity.Holiday::getDate).collect(java.util.stream.Collectors.toSet());

        int workingDays = 0;
        LocalDate d = start;
        while (!d.isAfter(end)) {
            java.time.DayOfWeek dow = d.getDayOfWeek();
            boolean isWeekend = dow == java.time.DayOfWeek.SATURDAY || dow == java.time.DayOfWeek.SUNDAY;
            boolean isHoliday = holidayDates.contains(d);
            if (!isWeekend && !isHoliday) workingDays++;
            d = d.plusDays(1);
        }
        return workingDays;
    }

    public List<LeaveRequestDTO> getAllLeaveRequests(Long clientId) {
        List<LeaveRequest> requests = leaveRequestRepository.findAllByEmployeeClientIdOrderByCreatedAtDesc(clientId);
        return requests.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public Page<LeaveRequestDTO> getLeaveRequestsPage(Long clientId, String status, Long searchId, String searchName,
                                                       Integer year, Integer month, Pageable pageable) {
        Page<LeaveRequest> requests = leaveRequestRepository.filterLeaveRequests(
                clientId, status, searchId, searchName, year, month, pageable);
        return requests.map(this::convertToDTO);
    }

    public List<LeaveRequestDTO> getLeaveRequestsByEmployeeId(Long empId, Long clientId) {
        Employee employee = employeeRepository.findById(empId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "empId", empId));
        if (employee.getClient() == null || !employee.getClient().getId().equals(clientId)) {
            throw new ResourceNotFoundException("Employee", "empId", empId);
        }
        List<LeaveRequest> requests = leaveRequestRepository.findByEmployeeEmpIdOrderByCreatedAtDesc(empId);
        return requests.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public List<LeaveRequestDTO> getLeaveRequestsForEmployees(java.util.Collection<Long> empIds) {
        if (empIds == null || empIds.isEmpty()) {
            return List.of();
        }
        List<LeaveRequest> requests = leaveRequestRepository.findByEmployeeEmpIdInOrderByCreatedAtDesc(empIds);
        return requests.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public LeaveRequestDTO updateLeaveRequestStatus(Long requestId, UpdateLeaveRequestStatusDTO statusDTO, Long clientId) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("LeaveRequest", "id", requestId));
        if (clientId != null && !ownedByClient(leaveRequest, clientId)) {
            throw new ResourceNotFoundException("LeaveRequest", "id", requestId);
        }

        String oldStatus = leaveRequest.getStatus();

        // preserve original days in case we need to restore
        Integer originalDays = leaveRequest.getDays();

        // compute working days from dates (exclude weekends and holidays)
        Employee requestEmployee = leaveRequest.getEmployee();
        Long requestClientId = requestEmployee != null && requestEmployee.getClient() != null
                ? requestEmployee.getClient().getId() : null;
        int workingDaysFromDates = calculateWorkingDays(leaveRequest.getFromDate(), leaveRequest.getToDate(), requestClientId);

        // Prefer the days already stored on the leave request (created working days).
        // If missing, fall back to computed working days. Ignore any incorrect `days` value sent in DTO.
        int effectiveDays = (leaveRequest.getDays() != null && leaveRequest.getDays() > 0) ? leaveRequest.getDays() : workingDaysFromDates;
        leaveRequest.setDays(effectiveDays);

        String newStatus = normalizeStatus(statusDTO.getStatus());
        leaveRequest.setStatus(newStatus);

        LeaveBalance leaveBalance = leaveBalanceRepository
                .findByEmployeeEmpIdAndLeaveType(leaveRequest.getEmployee().getEmpId(), leaveRequest.getLeaveType())
                .orElseThrow(() -> new RuntimeException("Leave balance not found"));

        // Approve flow
        if ("Approved".equals(newStatus)) {
            // Transition from non-approved -> approved: deduct full days
            if (!"Approved".equals(oldStatus)) {
                int deduct = effectiveDays;
                if (leaveBalance.getBalance() < deduct) {
                    throw new RuntimeException("Insufficient leave balance. Available: " + leaveBalance.getBalance() + " days, Requested: " + deduct + " days");
                }
                leaveBalance.setBalance(leaveBalance.getBalance() - deduct);
                leaveBalanceRepository.save(leaveBalance);

                LeaveHistory history = new LeaveHistory(
                        leaveRequest.getEmployee(),
                        leaveRequest.getLeaveType(),
                        deduct,
                        leaveRequest.getYear()
                );
                leaveHistoryRepository.save(history);
            } else {
                // Already approved and still approved: adjust by delta if days changed
                int newDays = effectiveDays;
                int delta = newDays - (originalDays == null ? 0 : originalDays);
                if (delta > 0) {
                    if (leaveBalance.getBalance() < delta) {
                        throw new RuntimeException("Insufficient leave balance for adjustment. Available: " + leaveBalance.getBalance() + " days, Additional requested: " + delta + " days");
                    }
                    leaveBalance.setBalance(leaveBalance.getBalance() - delta);
                    leaveBalanceRepository.save(leaveBalance);

                    LeaveHistory history = new LeaveHistory(
                            leaveRequest.getEmployee(),
                            leaveRequest.getLeaveType(),
                            delta,
                            leaveRequest.getYear()
                    );
                    leaveHistoryRepository.save(history);
                } else if (delta < 0) {
                    // reduction in approved days -> restore balance
                    leaveBalance.setBalance(leaveBalance.getBalance() + (-delta));
                    leaveBalanceRepository.save(leaveBalance);
                }
            }
        }

        // Rejection or cancellation: if it was previously approved, restore original days
        if (("Rejected".equals(newStatus) || "Cancelled".equals(newStatus)) && "Approved".equals(oldStatus)) {
            int restore = originalDays == null ? 0 : originalDays;
            leaveBalance.setBalance(leaveBalance.getBalance() + restore);
            leaveBalanceRepository.save(leaveBalance);
        }

        LeaveRequest updatedRequest = leaveRequestRepository.save(leaveRequest);

        if (!newStatus.equals(oldStatus) && ("Approved".equals(newStatus) || "Rejected".equals(newStatus))) {
            sendEmployeeStatusNotification(updatedRequest.getEmployee(), updatedRequest, newStatus);
        }

        return convertToDTO(updatedRequest);
    }

    private void sendEmployeeStatusNotification(Employee employee, LeaveRequest leaveRequest, String newStatus) {
        if (employee.getEmail() == null || employee.getEmail().trim().isEmpty()) {
            return;
        }

        String employeeName = String.format("%s %s",
                employee.getFirstName() == null ? "" : employee.getFirstName(),
                employee.getLastName() == null ? "" : employee.getLastName()).trim();

        emailService.sendHtmlEmail(employee.getEmail(), mailFrom, null,
                "Your Leave Request has been " + newStatus,
                buildEmployeeStatusBody(employeeName, leaveRequest, newStatus));
    }

    private String buildEmployeeStatusBody(String employeeName, LeaveRequest leaveRequest, String newStatus) {
        String statusMessage = "Approved".equals(newStatus)
                ? "Your leave request has been <strong>approved</strong>."
                : "Your leave request has been <strong>rejected</strong>.";

        return "<p>Hello " + employeeName + ",</p>"
                + "<p>" + statusMessage + "</p>"
                + "<ul>"
                + "<li><strong>Leave Type:</strong> " + leaveRequest.getLeaveType() + "</li>"
                + "<li><strong>From Date:</strong> " + leaveRequest.getFromDate().format(DISPLAY_DATE_FORMATTER) + "</li>"
                + "<li><strong>To Date:</strong> " + leaveRequest.getToDate().format(DISPLAY_DATE_FORMATTER) + "</li>"
                + "<li><strong>Status:</strong> " + newStatus + "</li>"
                + "</ul>"
                + "<p>If you have any questions, please contact HR.</p>";
    }

    // requestId here is caller-owned (ManagerController re-checks its own team membership before
    // calling this) except via LeaveRequestController.getLeaveRequestById, which passes clientId.
    public LeaveRequestDTO getLeaveRequestById(Long requestId) {
        return getLeaveRequestById(requestId, null);
    }

    public LeaveRequestDTO getLeaveRequestById(Long requestId, Long clientId) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("LeaveRequest", "id", requestId));
        if (clientId != null && !ownedByClient(leaveRequest, clientId)) {
            throw new ResourceNotFoundException("LeaveRequest", "id", requestId);
        }
        return convertToDTO(leaveRequest);
    }

    private boolean ownedByClient(LeaveRequest leaveRequest, Long clientId) {
        Employee employee = leaveRequest.getEmployee();
        return employee != null && employee.getClient() != null && employee.getClient().getId().equals(clientId);
    }

    public List<LeaveRequestDTO> getLeaveRequestsByStatus(String status, Long clientId) {
        return leaveRequestRepository.findAllByEmployeeClientIdOrderByCreatedAtDesc(clientId)
                .stream()
                .filter(req -> req.getStatus().equals(status))
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public com.employee.management.backend.dto.LeaveReportDTO getLeaveReport(Long empId, Long clientId) {
        Employee employee = employeeRepository.findById(empId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "empId", empId));
        if (employee.getClient() == null || !employee.getClient().getId().equals(clientId)) {
            throw new ResourceNotFoundException("Employee", "empId", empId);
        }

        com.employee.management.backend.dto.LeaveReportDTO report = new com.employee.management.backend.dto.LeaveReportDTO(
                employee.getEmpId(),
                employee.getFirstName() + " " + employee.getLastName()
        );

        // Get all leave balances for this employee
        List<LeaveBalance> balances = leaveBalanceRepository.findByEmployeeEmpId(empId);
        for (LeaveBalance balance : balances) {
            report.addLeaveType(balance.getLeaveType(), balance.getBalance());
        }

        return report;
    }

    private LeaveRequestDTO convertToDTO(LeaveRequest leaveRequest) {
        return new LeaveRequestDTO(
                leaveRequest.getId(),
                leaveRequest.getEmployee().getEmpId(),
                leaveRequest.getEmployee().getFirstName() + " " + leaveRequest.getEmployee().getLastName(),
                leaveRequest.getLeaveType(),
                leaveRequest.getDays(),
                leaveRequest.getYear(),
                leaveRequest.getCreatedAt().toString(),
                leaveRequest.getFromDate().toString(),
                leaveRequest.getToDate().toString(),
                leaveRequest.getStatus(),
                leaveRequest.getReason()
        );
    }
}
