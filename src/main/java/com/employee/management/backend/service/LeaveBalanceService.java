package com.employee.management.backend.service;

import com.employee.management.backend.Entity.Employee;
import com.employee.management.backend.Entity.LeaveBalance;
import com.employee.management.backend.dto.LeaveBalanceDTO;
import com.employee.management.backend.exception.ResourceNotFoundException;
import com.employee.management.backend.repository.EmployeeRepository;
import com.employee.management.backend.repository.LeaveBalanceRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class LeaveBalanceService {

    private final LeaveBalanceRepository leaveBalanceRepository;
    private final EmployeeRepository employeeRepository;

    public LeaveBalanceService(LeaveBalanceRepository leaveBalanceRepository,
                               EmployeeRepository employeeRepository) {
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.employeeRepository = employeeRepository;
    }

    public List<LeaveBalanceDTO> getLeaveBalancesByEmployeeId(Long empId, Long clientId) {
        Employee employee = employeeRepository.findById(empId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "empId", empId));
        if (employee.getClient() == null || !employee.getClient().getId().equals(clientId)) {
            throw new ResourceNotFoundException("Employee", "empId", empId);
        }

        List<LeaveBalance> balances = leaveBalanceRepository.findByEmployeeEmpId(empId);
        return balances.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public LeaveBalanceDTO getLeaveBalance(Long empId, String leaveType, Long clientId) {
        LeaveBalance balance = leaveBalanceRepository
                .findByEmployeeEmpIdAndLeaveType(empId, leaveType)
                .orElseThrow(() -> new RuntimeException("Leave balance not found for employee " + empId + " and type " + leaveType));
        if (!ownedByClient(balance, clientId)) {
            throw new RuntimeException("Leave balance not found for employee " + empId + " and type " + leaveType);
        }
        return convertToDTO(balance);
    }

    public LeaveBalanceDTO updateLeaveBalance(Long empId, String leaveType, Integer newBalance, Long clientId) {
        LeaveBalance balance = leaveBalanceRepository
                .findByEmployeeEmpIdAndLeaveType(empId, leaveType)
                .orElseThrow(() -> new RuntimeException("Leave balance not found"));
        if (!ownedByClient(balance, clientId)) {
            throw new RuntimeException("Leave balance not found");
        }

        balance.setBalance(newBalance);
        LeaveBalance updated = leaveBalanceRepository.save(balance);
        return convertToDTO(updated);
    }

    public List<LeaveBalanceDTO> getAllLeaveBalances(Long clientId) {
        List<LeaveBalance> balances = leaveBalanceRepository.findByEmployeeClientId(clientId);
        return balances.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    private boolean ownedByClient(LeaveBalance balance, Long clientId) {
        Employee employee = balance.getEmployee();
        return employee != null && employee.getClient() != null && employee.getClient().getId().equals(clientId);
    }

    private LeaveBalanceDTO convertToDTO(LeaveBalance balance) {
        return new LeaveBalanceDTO(
                balance.getId(),
                balance.getEmployee().getEmpId(),
                balance.getLeaveType(),
                balance.getBalance()
        );
    }
}
