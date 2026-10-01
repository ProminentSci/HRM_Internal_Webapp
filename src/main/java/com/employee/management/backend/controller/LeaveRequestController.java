package com.employee.management.backend.controller;

import com.employee.management.backend.dto.CreateLeaveRequestDTO;
import com.employee.management.backend.dto.LeaveReportDTO;
import com.employee.management.backend.dto.LeaveRequestDTO;
import com.employee.management.backend.dto.UpdateLeaveRequestStatusDTO;
import com.employee.management.backend.security.SecurityUtils;
import com.employee.management.backend.service.LeaveRequestService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leave-requests")
public class LeaveRequestController {

    private final LeaveRequestService leaveRequestService;

    public LeaveRequestController(LeaveRequestService leaveRequestService) {
        this.leaveRequestService = leaveRequestService;
    }

    @GetMapping
    public ResponseEntity<?> getAllLeaveRequests(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        boolean hasSearch = search != null && !search.trim().isEmpty();
        boolean hasMonthYear = year != null && month != null;
        boolean paginated = page != null || size != null || (status != null && !status.trim().isEmpty())
                || hasSearch || hasMonthYear;
        if (paginated) {
            int normalizedPage = Math.max(page != null ? page : 0, 0);
            int normalizedSize = Math.max(size != null ? size : 10, 1);
            String normalizedStatus = (status == null || status.trim().isEmpty() || "all".equalsIgnoreCase(status.trim()))
                    ? null : status.trim();

            Long searchId = null;
            String searchName = null;
            if (hasSearch) {
                String trimmedSearch = search.trim();
                if (trimmedSearch.matches("\\d+")) {
                    searchId = Long.parseLong(trimmedSearch);
                } else {
                    searchName = trimmedSearch;
                }
            }

            Page<LeaveRequestDTO> result = leaveRequestService.getLeaveRequestsPage(
                    SecurityUtils.currentClientId(), normalizedStatus, searchId, searchName, year, month,
                    PageRequest.of(normalizedPage, normalizedSize));
            return ResponseEntity.ok(result);
        }
        List<LeaveRequestDTO> requests = leaveRequestService.getAllLeaveRequests(SecurityUtils.currentClientId());
        return ResponseEntity.ok(requests);
    }

    @GetMapping("/employee/{empId}")
    public ResponseEntity<List<LeaveRequestDTO>> getLeaveRequestsByEmployeeId(@PathVariable Long empId) {
        List<LeaveRequestDTO> requests = leaveRequestService.getLeaveRequestsByEmployeeId(empId, SecurityUtils.currentClientId());
        return ResponseEntity.ok(requests);
    }

    @GetMapping("/{requestId}")
    public ResponseEntity<LeaveRequestDTO> getLeaveRequestById(@PathVariable Long requestId) {
        LeaveRequestDTO request = leaveRequestService.getLeaveRequestById(requestId, SecurityUtils.currentClientId());
        return ResponseEntity.ok(request);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<LeaveRequestDTO>> getLeaveRequestsByStatus(@PathVariable String status) {
        List<LeaveRequestDTO> requests = leaveRequestService.getLeaveRequestsByStatus(status, SecurityUtils.currentClientId());
        return ResponseEntity.ok(requests);
    }

    @GetMapping("/report/{empId}")
    public ResponseEntity<?> getLeaveReport(@PathVariable Long empId) {
        try {
            LeaveReportDTO report = leaveRequestService.getLeaveReport(empId, SecurityUtils.currentClientId());
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> createLeaveRequest(@RequestBody CreateLeaveRequestDTO requestDTO) {
        try {
            LeaveRequestDTO createdRequest = leaveRequestService.createLeaveRequest(requestDTO);
            return ResponseEntity.status(201).body(createdRequest);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{requestId}/status")
    public ResponseEntity<?> updateLeaveRequestStatus(
            @PathVariable Long requestId,
            @RequestBody UpdateLeaveRequestStatusDTO statusDTO) {
        try {
            LeaveRequestDTO updatedRequest = leaveRequestService.updateLeaveRequestStatus(requestId, statusDTO, SecurityUtils.currentClientId());
            return ResponseEntity.ok(updatedRequest);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    static class ErrorResponse {
        private String message;

        public ErrorResponse(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
