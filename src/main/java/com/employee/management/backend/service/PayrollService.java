package com.employee.management.backend.service;

import com.employee.management.backend.Entity.Employee;
import com.employee.management.backend.Entity.LeaveRequest;
import com.employee.management.backend.Entity.Payroll;
import com.employee.management.backend.Entity.SalaryDetails;
import com.employee.management.backend.dto.DocumentFile;
import com.employee.management.backend.dto.PayrollEmployeeRequestDTO;
import com.employee.management.backend.dto.PayrollEmployeeResponseDTO;
import com.employee.management.backend.dto.PayrollProcessRequestDTO;
import com.employee.management.backend.dto.PayrollProcessResponseDTO;
import com.employee.management.backend.repository.EmployeeRepository;
import com.employee.management.backend.repository.HolidayRepository;
import com.employee.management.backend.repository.LeaveRequestRepository;
import com.employee.management.backend.repository.PayrollRepository;
import com.employee.management.backend.security.AuthenticatedUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PayrollService {

    private final EmployeeRepository employeeRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final PayrollRepository payrollRepository;
    private final HolidayRepository holidayRepository;

    public PayrollService(EmployeeRepository employeeRepository,
                          LeaveRequestRepository leaveRequestRepository,
                          PayrollRepository payrollRepository,
                          HolidayRepository holidayRepository) {
        this.employeeRepository = employeeRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.payrollRepository = payrollRepository;
        this.holidayRepository = holidayRepository;
    }

    @Transactional
    public PayrollProcessResponseDTO processPayroll(PayrollProcessRequestDTO request) {
        YearMonth payrollMonth = resolvePayrollMonth(request);
        PayrollProcessResponseDTO response = new PayrollProcessResponseDTO();
        response.setMonth(payrollMonth.getMonthValue());
        response.setYear(payrollMonth.getYear());

        List<PayrollEmployeeRequestDTO> requestedEmployees = request.getEmployees();
        for (PayrollEmployeeRequestDTO requestedEmployee : requestedEmployees) {
            PayrollEmployeeResponseDTO employeePayroll = processEmployeePayroll(requestedEmployee, payrollMonth);
            response.getEmployees().add(employeePayroll);
        }

        response.setTotalEmployees(response.getEmployees().size());
        response.setTotalGrossSalary(round(response.getEmployees().stream()
                .mapToDouble(employee -> valueOrZero(employee.getMonthlyGrossSalary()))
                .sum()));
        response.setTotalLeaveDeduction(round(response.getEmployees().stream()
                .mapToDouble(employee -> valueOrZero(employee.getLeaveDeduction()))
                .sum()));
        response.setTotalNetSalary(round(response.getEmployees().stream()
                .mapToDouble(employee -> valueOrZero(employee.getNetSalary()))
                .sum()));

        return response;
    }

    // Scoped to one client (the caller's own company) but otherwise unfiltered/unpaginated -
    // used by the Excel export, which legitimately wants every matching row in one file.
    @Transactional(readOnly = true)
    public PayrollProcessResponseDTO getProcessedPayrollByMonthAndYear(Long clientId, Integer month, Integer year) {
        return getProcessedPayrollByMonthAndYear(clientId, month, year, null, null, null);
    }

    @Transactional(readOnly = true)
    public PayrollProcessResponseDTO getProcessedPayrollByMonthAndYear(Long clientId, Integer month, Integer year,
                                                                        String status, Long searchId, String searchName) {
        PayrollProcessResponseDTO response = new PayrollProcessResponseDTO();
        response.setMonth(month);
        response.setYear(year);

        List<Payroll> payrolls = payrollRepository.filterForClient(clientId, month, year, status, searchId, searchName);
        for (Payroll payroll : payrolls) {
            response.getEmployees().add(convertPayrollToResponse(payroll));
        }

        response.setTotalEmployees(response.getEmployees().size());
        response.setTotalGrossSalary(round(response.getEmployees().stream()
                .mapToDouble(employee -> valueOrZero(employee.getMonthlyGrossSalary()))
                .sum()));
        response.setTotalLeaveDeduction(round(response.getEmployees().stream()
                .mapToDouble(employee -> valueOrZero(employee.getLeaveDeduction()))
                .sum()));
        response.setTotalNetSalary(round(response.getEmployees().stream()
                .mapToDouble(employee -> valueOrZero(employee.getNetSalary()))
                .sum()));
        long creditedCount = payrolls.stream().filter(p -> isCredited(p.getCreditStatus())).count();
        response.setCreditedCount((int) creditedCount);
        response.setPendingCount(response.getEmployees().size() - (int) creditedCount);

        return response;
    }

    @Transactional(readOnly = true)
    public List<Long> getProcessedEmployeeIds(Long clientId, Integer month, Integer year) {
        return payrollRepository.findProcessedEmployeeIds(clientId, month, year);
    }

    // Backs the employee-facing Payslip page's own lookup - the caller's empId comes from the
    // verified JWT (see PayrollController.getMyPayrollRecord), never a client-supplied id.
    @Transactional(readOnly = true)
    public Optional<PayrollEmployeeResponseDTO> getEmployeePayrollForMonth(Long empId, Integer month, Integer year) {
        return payrollRepository.findByEmployeeIdAndMonthAndYear(empId, month, year)
                .map(this::convertPayrollToResponse);
    }

    // Page-sliced on top of the full filtered result above - the aggregate totals/counts stay
    // correct for the whole filtered set (they can't be computed by the DB since gross salary
    // and leave deduction are derived from SalaryDetails/leave days, not stored columns), only
    // the `employees` list itself is trimmed down to the requested page.
    @Transactional(readOnly = true)
    public PayrollProcessResponseDTO getProcessedPayrollReportPage(Long clientId, Integer month, Integer year,
                                                                     String status, Long searchId, String searchName,
                                                                     int page, int size) {
        PayrollProcessResponseDTO response = getProcessedPayrollByMonthAndYear(
                clientId, month, year, status, searchId, searchName);

        List<PayrollEmployeeResponseDTO> allEmployees = response.getEmployees();
        int fromIndex = Math.min(page * size, allEmployees.size());
        int toIndex = Math.min(fromIndex + size, allEmployees.size());
        response.setEmployees(new ArrayList<>(allEmployees.subList(fromIndex, toIndex)));
        response.setPage(page);
        response.setSize(size);
        response.setTotalPages(Math.max(1, (int) Math.ceil(allEmployees.size() / (double) size)));

        return response;
    }

    @Transactional
    public PayrollEmployeeResponseDTO updatePayrollCreditStatus(Long payrollId,
                                                                Long employeeId,
                                                                Integer month,
                                                                Integer year,
                                                                String status) {
        if (status == null || status.trim().isEmpty()) {
            throw new RuntimeException("Status is required");
        }

        Payroll payroll = payrollRepository.findById(payrollId)
                .orElseGet(() -> findPayrollByEmployeeMonthAndYear(employeeId, month, year));

        payroll.setCreditStatus(status.trim());
        Payroll updatedPayroll = payrollRepository.save(payroll);
        return convertPayrollToResponse(updatedPayroll);
    }

    @Transactional
    public PayrollEmployeeResponseDTO updatePayslipMode(Long payrollId,
                                                         Long employeeId,
                                                         Integer month,
                                                         Integer year,
                                                         boolean manualPayslip) {
        Payroll payroll = payrollRepository.findById(payrollId)
                .orElseGet(() -> findPayrollByEmployeeMonthAndYear(employeeId, month, year));

        payroll.setManualPayslip(manualPayslip);
        Payroll updatedPayroll = payrollRepository.save(payroll);
        return convertPayrollToResponse(updatedPayroll);
    }

    @Transactional
    public PayrollEmployeeResponseDTO uploadManualPayslip(Long payrollId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("A payslip file is required");
        }

        Payroll payroll = payrollRepository.findById(payrollId)
                .orElseThrow(() -> new RuntimeException("Payroll record not found"));

        try {
            payroll.setPayslipData(file.getBytes());
        } catch (IOException ex) {
            throw new RuntimeException("Failed to read uploaded payslip file");
        }
        payroll.setPayslipFileName(file.getOriginalFilename());
        payroll.setPayslipContentType(file.getContentType());
        payroll.setManualPayslip(true);

        Payroll updatedPayroll = payrollRepository.save(payroll);
        return convertPayrollToResponse(updatedPayroll);
    }

    @Transactional(readOnly = true)
    public DocumentFile getManualPayslip(Long payrollId, AuthenticatedUser requester) {
        Payroll payroll = payrollRepository.findById(payrollId)
                .orElseThrow(() -> new RuntimeException("Payroll record not found"));

        boolean isAdmin = requester != null && "ADMIN".equalsIgnoreCase(requester.role());
        boolean isOwner = requester != null && requester.empId() != null
                && requester.empId().equals(payroll.getEmployeeId());
        if (!isAdmin && !isOwner) {
            throw new SecurityException("Not authorized to view this payslip");
        }

        if (!isCredited(payroll.getCreditStatus())) {
            throw new RuntimeException("Payslip is not available until the amount is credited");
        }

        if (payroll.getPayslipData() == null) {
            throw new RuntimeException("No payslip has been uploaded for this record");
        }

        return new DocumentFile(
                payroll.getPayslipFileName(),
                payroll.getPayslipContentType(),
                payroll.getPayslipData().length,
                payroll.getPayslipData()
        );
    }

    private boolean isCredited(String creditStatus) {
        if (creditStatus == null) {
            return false;
        }
        String value = creditStatus.trim().toLowerCase();
        return value.equals("credited")
                || value.equals("amount credited")
                || value.equals("amount_credited")
                || value.equals("paid")
                || value.equals("payment credited");
    }

    private PayrollEmployeeResponseDTO processEmployeePayroll(PayrollEmployeeRequestDTO requestedEmployee,
                                                              YearMonth payrollMonth) {
        PayrollEmployeeResponseDTO response = new PayrollEmployeeResponseDTO();
        if (requestedEmployee == null) {
            response.setStatus("FAILED");
            response.setMessage("Employee payload is required");
            return response;
        }

        response.setEmployeeId(requestedEmployee.getEmployeeId());
        response.setRequestedEmployeeName(requestedEmployee.getEmployeeName());

        if (requestedEmployee.getEmployeeId() == null) {
            response.setStatus("FAILED");
            response.setMessage("Employee id is required");
            return response;
        }

        Employee employee = employeeRepository.findById(requestedEmployee.getEmployeeId()).orElse(null);
        if (employee == null) {
            response.setStatus("FAILED");
            response.setMessage("Employee not found");
            return response;
        }

        response.setEmployeeName(buildEmployeeName(employee));
        SalaryDetails salaryDetails = employee.getSalaryDetails();
        if (salaryDetails == null) {
            response.setStatus("FAILED");
            response.setMessage("Salary details not found");
            return response;
        }

        double basicSalary = parseAmount(salaryDetails.getBasicSalary());
        double bonus = parseAmount(salaryDetails.getBonus());
        double ctc = parseAmount(salaryDetails.getCtc());
        double monthlyGrossSalary = ctc > 0 ? ctc / 12 : basicSalary + bonus;
        Long employeeClientId = employee.getClient() == null ? null : employee.getClient().getId();
        int unpaidLeaveDays = getApprovedLeaveDays(employee.getEmpId(), payrollMonth, true, employeeClientId);
        int paidLeaveDays = getApprovedLeaveDays(employee.getEmpId(), payrollMonth, false, employeeClientId);
        double dailySalary = monthlyGrossSalary / payrollMonth.lengthOfMonth();
        double leaveDeduction = dailySalary * unpaidLeaveDays;
        // A one-off top-up entered by the admin for this run only - added after the LOP
        // deduction, not prorated against attendance like the recurring salary components.
        double variablePay = requestedEmployee.getVariablePay() != null ? requestedEmployee.getVariablePay() : 0;
        double netSalary = Math.max(0, monthlyGrossSalary - leaveDeduction) + variablePay;
        Payroll payroll = savePayroll(employee, salaryDetails, payrollMonth, unpaidLeaveDays, round(netSalary), round(variablePay));

        response.setStatus("PROCESSED");
        response.setMessage("Payroll processed successfully");
        response.setPayrollId(payroll.getId());
        response.setDateOfJoining(payroll.getDateOfJoining());
        response.setLop(payroll.getLop());
        response.setSalary(payroll.getSalary());
        response.setPanNumber(payroll.getPanNumber());
        response.setAccountNumber(payroll.getAccountNumber());
        response.setIfsc(payroll.getIfsc());
        response.setUan(payroll.getUan());
        response.setPf(payroll.getPf());
        response.setCreditStatus(payroll.getCreditStatus());
        response.setMonth(payroll.getMonth());
        response.setYear(payroll.getYear());
        response.setBasicSalary(round(basicSalary));
        response.setBonus(round(bonus));
        response.setCtc(round(ctc));
        response.setMonthlyGrossSalary(round(monthlyGrossSalary));
        response.setPaidLeaveDays(paidLeaveDays);
        response.setUnpaidLeaveDays(unpaidLeaveDays);
        response.setLeaveDeduction(round(leaveDeduction));
        response.setVariablePay(round(variablePay));
        response.setNetSalary(round(netSalary));

        return response;
    }

    private Payroll savePayroll(Employee employee,
                                SalaryDetails salaryDetails,
                                YearMonth payrollMonth,
                                int lop,
                                double salary,
                                double variablePay) {
        Payroll payroll = payrollRepository
                .findByEmployeeIdAndMonthAndYear(employee.getEmpId(), payrollMonth.getMonthValue(), payrollMonth.getYear())
                .orElseGet(Payroll::new);

        payroll.setEmployeeId(employee.getEmpId());
        payroll.setEmployeeName(buildEmployeeName(employee));
        payroll.setDateOfJoining(employee.getJobDetails() == null ? null : employee.getJobDetails().getDateOfJoining());
        payroll.setLop(lop);
        payroll.setSalary(salary);
        payroll.setVariablePay(variablePay);
        payroll.setPanNumber(salaryDetails.getPanNumber());
        payroll.setAccountNumber(salaryDetails.getAccountNumber());
        payroll.setIfsc(salaryDetails.getIfscCode());
        payroll.setUan(salaryDetails.getUanNumber());
        payroll.setPf(salaryDetails.getPfNumber());
        if (payroll.getCreditStatus() == null || payroll.getCreditStatus().trim().isEmpty()) {
            payroll.setCreditStatus("NON_CREDITED");
        }
        payroll.setMonth(payrollMonth.getMonthValue());
        payroll.setYear(payrollMonth.getYear());

        return payrollRepository.save(payroll);
    }

    private PayrollEmployeeResponseDTO convertPayrollToResponse(Payroll payroll) {
        PayrollEmployeeResponseDTO response = new PayrollEmployeeResponseDTO();
        response.setPayrollId(payroll.getId());
        response.setEmployeeId(payroll.getEmployeeId());
        response.setEmployeeName(payroll.getEmployeeName());
        response.setRequestedEmployeeName(payroll.getEmployeeName());
        response.setStatus("PROCESSED");
        response.setMessage("Payroll data fetched from database");
        response.setDateOfJoining(payroll.getDateOfJoining());
        response.setLop(payroll.getLop());
        response.setSalary(payroll.getSalary());
        response.setNetSalary(payroll.getSalary());
        response.setVariablePay(payroll.getVariablePay());
        response.setPanNumber(payroll.getPanNumber());
        response.setAccountNumber(payroll.getAccountNumber());
        response.setIfsc(payroll.getIfsc());
        response.setUan(payroll.getUan());
        response.setPf(payroll.getPf());
        response.setCreditStatus(payroll.getCreditStatus());
        response.setMonth(payroll.getMonth());
        response.setYear(payroll.getYear());
        response.setManualPayslip(payroll.isManualPayslip());
        response.setHasPayslipFile(payroll.getPayslipData() != null);

        Employee employee = employeeRepository.findById(payroll.getEmployeeId()).orElse(null);
        if (employee != null && employee.getSalaryDetails() != null) {
            SalaryDetails salaryDetails = employee.getSalaryDetails();
            YearMonth payrollMonth = YearMonth.of(payroll.getYear(), payroll.getMonth());
            double basicSalary = parseAmount(salaryDetails.getBasicSalary());
            double bonus = parseAmount(salaryDetails.getBonus());
            double ctc = parseAmount(salaryDetails.getCtc());
            double monthlyGrossSalary = ctc > 0 ? ctc / 12 : basicSalary + bonus;
            Long employeeClientId = employee.getClient() == null ? null : employee.getClient().getId();
            int unpaidLeaveDays = getApprovedLeaveDays(employee.getEmpId(), payrollMonth, true, employeeClientId);
            int paidLeaveDays = getApprovedLeaveDays(employee.getEmpId(), payrollMonth, false, employeeClientId);
            double dailySalary = monthlyGrossSalary / payrollMonth.lengthOfMonth();
            double leaveDeduction = dailySalary * unpaidLeaveDays;

            response.setBasicSalary(round(basicSalary));
            response.setBonus(round(bonus));
            response.setCtc(round(ctc));
            response.setMonthlyGrossSalary(round(monthlyGrossSalary));
            response.setPaidLeaveDays(paidLeaveDays);
            response.setUnpaidLeaveDays(unpaidLeaveDays);
            response.setLeaveDeduction(round(leaveDeduction));
        } else {
            response.setMonthlyGrossSalary(payroll.getSalary());
            response.setLeaveDeduction(0.0);
        }
        return response;
    }

    private Payroll findPayrollByEmployeeMonthAndYear(Long employeeId, Integer month, Integer year) {
        if (employeeId == null || month == null || year == null) {
            throw new RuntimeException("Payroll record not found");
        }
        return payrollRepository.findByEmployeeIdAndMonthAndYear(employeeId, month, year)
                .orElseThrow(() -> new RuntimeException("Payroll record not found"));
    }

    private YearMonth resolvePayrollMonth(PayrollProcessRequestDTO request) {
        LocalDate today = LocalDate.now();
        int month = request.getMonth() == null ? today.getMonthValue() : request.getMonth();
        int year = request.getYear() == null ? today.getYear() : request.getYear();
        return YearMonth.of(year, month);
    }

    private int getApprovedLeaveDays(Long empId, YearMonth payrollMonth, boolean unpaidOnly, Long clientId) {
        return leaveRequestRepository.findByEmployeeEmpIdOrderByCreatedAtDesc(empId).stream()
                .filter(leaveRequest -> "Approved".equalsIgnoreCase(leaveRequest.getStatus()))
                .filter(leaveRequest -> overlapsPayrollMonth(leaveRequest, payrollMonth))
                .filter(leaveRequest -> unpaidOnly == isUnpaidLeave(leaveRequest.getLeaveType()))
                .mapToInt(leaveRequest -> countLeaveDaysInMonth(leaveRequest, payrollMonth, clientId))
                .sum();
    }

    private boolean overlapsPayrollMonth(LeaveRequest leaveRequest, YearMonth payrollMonth) {
        LocalDate monthStart = payrollMonth.atDay(1);
        LocalDate monthEnd = payrollMonth.atEndOfMonth();
        return !leaveRequest.getToDate().isBefore(monthStart) && !leaveRequest.getFromDate().isAfter(monthEnd);
    }

    private int countLeaveDaysInMonth(LeaveRequest leaveRequest, YearMonth payrollMonth, Long clientId) {
        LocalDate start = leaveRequest.getFromDate().isBefore(payrollMonth.atDay(1))
                ? payrollMonth.atDay(1)
                : leaveRequest.getFromDate();
        LocalDate end = leaveRequest.getToDate().isAfter(payrollMonth.atEndOfMonth())
                ? payrollMonth.atEndOfMonth()
                : leaveRequest.getToDate();
        return countWorkingDays(start, end, clientId);
    }

    private int countWorkingDays(LocalDate start, LocalDate end, Long clientId) {
        if (start.isAfter(end)) {
            return 0;
        }

        List<com.employee.management.backend.Entity.Holiday> holidaysInRange = clientId == null
                ? List.of()
                : holidayRepository.findByClientIdAndDateBetweenOrderByDateAsc(clientId, start, end);
        Set<LocalDate> holidayDates = holidaysInRange.stream()
                .map(com.employee.management.backend.Entity.Holiday::getDate)
                .collect(Collectors.toSet());

        int workingDays = 0;
        LocalDate current = start;
        while (!current.isAfter(end)) {
            boolean isWeekend = current.getDayOfWeek() == DayOfWeek.SATURDAY || current.getDayOfWeek() == DayOfWeek.SUNDAY;
            if (!isWeekend && !holidayDates.contains(current)) {
                workingDays++;
            }
            current = current.plusDays(1);
        }
        return workingDays;
    }

    private boolean isUnpaidLeave(String leaveType) {
        if (leaveType == null) {
            return false;
        }
        String normalizedLeaveType = leaveType.trim().toLowerCase();
        return normalizedLeaveType.equals("unpaid")
                || normalizedLeaveType.equals("lop")
                || normalizedLeaveType.equals("loss of pay");
    }

    private String buildEmployeeName(Employee employee) {
        return String.format("%s %s",
                employee.getFirstName() == null ? "" : employee.getFirstName(),
                employee.getLastName() == null ? "" : employee.getLastName()).trim();
    }

    private double parseAmount(String amount) {
        if (amount == null || amount.trim().isEmpty()) {
            return 0;
        }
        try {
            return Double.parseDouble(amount.replace(",", "").trim());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private double valueOrZero(Double value) {
        return value == null ? 0 : value;
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
