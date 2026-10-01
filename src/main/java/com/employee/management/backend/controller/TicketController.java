package com.employee.management.backend.controller;

import com.employee.management.backend.dto.CreateTicketDTO;
import com.employee.management.backend.dto.TicketDTO;
import com.employee.management.backend.dto.UpdateTicketStatusDTO;
import com.employee.management.backend.security.AuthenticatedUser;
import com.employee.management.backend.service.TicketService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    private AuthenticatedUser currentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof AuthenticatedUser authenticatedUser)) {
            throw new RuntimeException("Not authenticated");
        }
        return authenticatedUser;
    }

    @PostMapping
    public ResponseEntity<?> createTicket(@RequestBody CreateTicketDTO dto) {
        try {
            TicketDTO created = ticketService.createTicket(currentUser().empId(), dto);
            return ResponseEntity.status(201).body(created);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/my")
    public ResponseEntity<List<TicketDTO>> getMyTickets() {
        return ResponseEntity.ok(ticketService.getTicketsForEmployee(currentUser().empId()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<Page<TicketDTO>> getClientTickets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) String status) {
        Page<TicketDTO> result = ticketService.getTicketsForClientPage(
                currentUser().clientId(), status, PageRequest.of(Math.max(page, 0), Math.max(size, 1)));
        return ResponseEntity.ok(result);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{ticketId}/status")
    public ResponseEntity<?> updateTicketStatus(
            @PathVariable Long ticketId,
            @RequestBody UpdateTicketStatusDTO dto) {
        try {
            TicketDTO updated = ticketService.updateTicketStatus(ticketId, currentUser().clientId(), dto);
            return ResponseEntity.ok(updated);
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
