package com.employee.management.backend.service;

import com.employee.management.backend.Entity.LeaveBalance;
import com.employee.management.backend.Entity.LeavePolicySettings;
import com.employee.management.backend.dto.LeavePolicySettingsDTO;
import com.employee.management.backend.repository.LeaveBalanceRepository;
import com.employee.management.backend.repository.LeavePolicySettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class LeavePolicySettingsService {

    // Order matches the LeaveBalance.leaveType rows seeded for every employee - getAllocations()
    // below returns allocation counts zipped to this same order.
    public static final String[] LEAVE_TYPES = {"Casual", "Sick", "Paid"};

    private static final int DEFAULT_CASUAL = 12;
    private static final int DEFAULT_SICK = 8;
    private static final int DEFAULT_PAID = 20;

    private final LeavePolicySettingsRepository leavePolicySettingsRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;

    public LeavePolicySettingsService(LeavePolicySettingsRepository leavePolicySettingsRepository,
                                       LeaveBalanceRepository leaveBalanceRepository) {
        this.leavePolicySettingsRepository = leavePolicySettingsRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
    }

    @Transactional(readOnly = true)
    public LeavePolicySettingsDTO getSettings(Long clientId) {
        return toDto(findOrDefault(clientId));
    }

    @Transactional
    public LeavePolicySettingsDTO updateSettings(Long clientId, LeavePolicySettingsDTO dto) {
        if (dto == null) {
            throw new RuntimeException("Request body is required");
        }

        int casual = valueOrDefault(dto.getCasualLeaveAllocation(), DEFAULT_CASUAL);
        int sick = valueOrDefault(dto.getSickLeaveAllocation(), DEFAULT_SICK);
        int paid = valueOrDefault(dto.getPaidLeaveAllocation(), DEFAULT_PAID);

        if (casual < 0 || sick < 0 || paid < 0) {
            throw new RuntimeException("Leave allocations cannot be negative");
        }

        LeavePolicySettings settings = leavePolicySettingsRepository.findByClientId(clientId).orElseGet(() -> {
            LeavePolicySettings created = new LeavePolicySettings();
            created.setClientId(clientId);
            return created;
        });

        // Capture what every employee's balance was seeded/last-adjusted against, before
        // overwriting - existing balances are shifted by the delta below rather than reset to
        // the new number outright, so leave already taken this year is preserved.
        int previousCasual = settings.getCasualLeaveAllocation();
        int previousSick = settings.getSickLeaveAllocation();
        int previousPaid = settings.getPaidLeaveAllocation();

        settings.setCasualLeaveAllocation(casual);
        settings.setSickLeaveAllocation(sick);
        settings.setPaidLeaveAllocation(paid);

        LeavePolicySettingsDTO result = toDto(leavePolicySettingsRepository.save(settings));

        applyAllocationDeltas(clientId, Map.of(
                "Casual", casual - previousCasual,
                "Sick", sick - previousSick,
                "Paid", paid - previousPaid
        ));

        return result;
    }

    // Shifts every existing LeaveBalance row of the client by the same delta as its allocation
    // change (balance = allocation - used, so shifting by the allocation delta keeps "used"
    // unchanged) instead of overwriting balances to the new allocation outright, which would
    // silently erase however many days an employee had already taken this year.
    private void applyAllocationDeltas(Long clientId, Map<String, Integer> deltasByLeaveType) {
        List<LeaveBalance> toUpdate = new ArrayList<>();
        for (LeaveBalance balance : leaveBalanceRepository.findByEmployeeClientId(clientId)) {
            Integer delta = deltasByLeaveType.get(balance.getLeaveType());
            if (delta != null && delta != 0) {
                balance.setBalance(Math.max(0, balance.getBalance() + delta));
                toUpdate.add(balance);
            }
        }
        if (!toUpdate.isEmpty()) {
            leaveBalanceRepository.saveAll(toUpdate);
        }
    }

    // Allocation counts in LEAVE_TYPES order, for seeding a client's employees' LeaveBalance rows.
    @Transactional(readOnly = true)
    public int[] getAllocations(Long clientId) {
        LeavePolicySettings settings = findOrDefault(clientId);
        return new int[]{settings.getCasualLeaveAllocation(), settings.getSickLeaveAllocation(), settings.getPaidLeaveAllocation()};
    }

    private LeavePolicySettings findOrDefault(Long clientId) {
        return leavePolicySettingsRepository.findByClientId(clientId).orElseGet(() -> {
            LeavePolicySettings defaults = new LeavePolicySettings();
            defaults.setClientId(clientId);
            return defaults;
        });
    }

    private LeavePolicySettingsDTO toDto(LeavePolicySettings settings) {
        return new LeavePolicySettingsDTO(
                settings.getCasualLeaveAllocation(),
                settings.getSickLeaveAllocation(),
                settings.getPaidLeaveAllocation()
        );
    }

    private int valueOrDefault(Integer value, int fallback) {
        return value != null ? value : fallback;
    }
}
