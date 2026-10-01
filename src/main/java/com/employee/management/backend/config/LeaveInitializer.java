package com.employee.management.backend.config;

import com.employee.management.backend.Entity.Employee;
import com.employee.management.backend.Entity.LeaveBalance;
import com.employee.management.backend.repository.EmployeeRepository;
import com.employee.management.backend.repository.LeaveBalanceRepository;
import com.employee.management.backend.service.LeavePolicySettingsService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class LeaveInitializer implements CommandLineRunner {

    private final EmployeeRepository employeeRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final LeavePolicySettingsService leavePolicySettingsService;

    public LeaveInitializer(EmployeeRepository employeeRepository,
                            LeaveBalanceRepository leaveBalanceRepository,
                            LeavePolicySettingsService leavePolicySettingsService) {
        this.employeeRepository = employeeRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.leavePolicySettingsService = leavePolicySettingsService;
    }

    @Override
    public void run(String... args) throws Exception {
        // Get all employees
        List<Employee> employees = employeeRepository.findAll();

        String[] leaveTypes = LeavePolicySettingsService.LEAVE_TYPES;
        // Cache each client's allocations so employees of the same org don't re-query settings.
        Map<Long, int[]> allocationsByClient = new HashMap<>();

        for (Employee employee : employees) {
            Long clientId = employee.getClient() != null ? employee.getClient().getId() : null;
            int[] leaveAllocations = allocationsByClient.computeIfAbsent(clientId, leavePolicySettingsService::getAllocations);

            for (int i = 0; i < leaveTypes.length; i++) {
                // Check if leave balance already exists
                var existing = leaveBalanceRepository
                        .findByEmployeeEmpIdAndLeaveType(employee.getEmpId(), leaveTypes[i]);

                if (existing.isEmpty()) {
                    // Create new leave balance
                    LeaveBalance leaveBalance = new LeaveBalance();
                    leaveBalance.setEmployee(employee);
                    leaveBalance.setLeaveType(leaveTypes[i]);
                    leaveBalance.setBalance(leaveAllocations[i]);
                    leaveBalanceRepository.save(leaveBalance);
                }
            }
        }
    }
}
