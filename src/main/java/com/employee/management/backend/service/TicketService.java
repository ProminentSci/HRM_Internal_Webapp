package com.employee.management.backend.service;

import com.employee.management.backend.Entity.Employee;
import com.employee.management.backend.Entity.Ticket;
import com.employee.management.backend.dto.CreateTicketDTO;
import com.employee.management.backend.dto.TicketDTO;
import com.employee.management.backend.dto.UpdateTicketStatusDTO;
import com.employee.management.backend.exception.ResourceNotFoundException;
import com.employee.management.backend.repository.EmployeeRepository;
import com.employee.management.backend.repository.TicketRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class TicketService {

    private static final DateTimeFormatter DISPLAY_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    private final TicketRepository ticketRepository;
    private final EmployeeRepository employeeRepository;
    private final EmailService emailService;
    private final String mailFrom;
    private final String adminTicketDashboardUrl;

    public TicketService(TicketRepository ticketRepository,
                          EmployeeRepository employeeRepository,
                          EmailService emailService,
                          @Value("${app.mail.from}") String mailFrom,
                          @Value("${app.frontend-url}") String frontendUrl,
                          @Value("${app.admin.ticket-dashboard-path}") String adminTicketDashboardPath) {
        this.ticketRepository = ticketRepository;
        this.employeeRepository = employeeRepository;
        this.emailService = emailService;
        this.mailFrom = mailFrom;
        this.adminTicketDashboardUrl = frontendUrl + adminTicketDashboardPath;
    }

    public TicketDTO createTicket(Long empId, CreateTicketDTO dto) {
        Employee employee = employeeRepository.findById(empId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "empId", empId));

        if (dto.getSubject() == null || dto.getSubject().trim().isEmpty()) {
            throw new RuntimeException("Subject is required");
        }
        if (dto.getDescription() == null || dto.getDescription().trim().isEmpty()) {
            throw new RuntimeException("Description is required");
        }

        Ticket ticket = new Ticket();
        ticket.setEmployee(employee);
        ticket.setSubject(dto.getSubject().trim());
        ticket.setDescription(dto.getDescription().trim());
        ticket.setStatus("Open");
        LocalDateTime now = LocalDateTime.now();
        ticket.setCreatedAt(now);
        ticket.setUpdatedAt(now);

        Ticket saved = ticketRepository.save(ticket);
        sendAdminTicketNotification(employee, saved);
        return convertToDTO(saved);
    }

    private void sendAdminTicketNotification(Employee employee, Ticket ticket) {
        if (employee.getClient() == null) {
            return;
        }

        List<Employee> admins = employeeRepository.findByClientIdAndRoleIgnoreCase(employee.getClient().getId(), "ADMIN");
        if (admins.isEmpty()) {
            return;
        }

        String employeeName = String.format("%s %s",
                employee.getFirstName() == null ? "" : employee.getFirstName(),
                employee.getLastName() == null ? "" : employee.getLastName()).trim();

        String replyTo = employee.getEmail() != null && !employee.getEmail().trim().isEmpty()
                ? employee.getEmail() : null;

        String body = buildAdminNotificationBody(employeeName, employee, ticket);

        for (Employee admin : admins) {
            if (admin.getEmail() == null || admin.getEmail().trim().isEmpty()) {
                continue;
            }
            emailService.sendHtmlEmail(admin.getEmail(), mailFrom, replyTo,
                    "New Support Ticket from " + employeeName, body);
        }
    }

    private String buildAdminNotificationBody(String employeeName, Employee employee, Ticket ticket) {
        return "<p>Hello,</p>"
                + "<p>A new support ticket has been raised and is awaiting your review:</p>"
                + "<ul>"
                + "<li><strong>Employee Name:</strong> " + employeeName + "</li>"
                + "<li><strong>Employee ID:</strong> " + employee.getEmpId() + "</li>"
                + "<li><strong>Subject:</strong> " + ticket.getSubject() + "</li>"
                + "<li><strong>Description:</strong> " + ticket.getDescription() + "</li>"
                + "</ul>"
                + "<p><a href=\"" + adminTicketDashboardUrl + "\">Review this ticket on the Tickets dashboard</a></p>";
    }

    public List<TicketDTO> getTicketsForEmployee(Long empId) {
        return ticketRepository.findByEmployeeEmpIdOrderByCreatedAtDesc(empId)
                .stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public Page<TicketDTO> getTicketsForClientPage(Long clientId, String status, Pageable pageable) {
        String normalizedStatus = (status == null || status.isBlank() || "all".equalsIgnoreCase(status.trim()))
                ? null : status.trim();
        return ticketRepository.filterForClient(clientId, normalizedStatus, pageable).map(this::convertToDTO);
    }

    public TicketDTO updateTicketStatus(Long ticketId, Long adminClientId, UpdateTicketStatusDTO dto) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", ticketId));

        if (ticket.getEmployee().getClient() == null
                || !ticket.getEmployee().getClient().getId().equals(adminClientId)) {
            throw new ResourceNotFoundException("Ticket", "id", ticketId);
        }

        String newStatus = normalizeStatus(dto.getStatus());
        String oldStatus = ticket.getStatus();
        ticket.setStatus(newStatus);
        if (dto.getAdminResponse() != null) {
            ticket.setAdminResponse(dto.getAdminResponse().trim());
        }
        ticket.setUpdatedAt(LocalDateTime.now());

        Ticket updated = ticketRepository.save(ticket);

        if (!newStatus.equals(oldStatus)) {
            sendEmployeeStatusNotification(updated.getEmployee(), updated, newStatus);
        }

        return convertToDTO(updated);
    }

    private void sendEmployeeStatusNotification(Employee employee, Ticket ticket, String newStatus) {
        if (employee.getEmail() == null || employee.getEmail().trim().isEmpty()) {
            return;
        }

        String employeeName = String.format("%s %s",
                employee.getFirstName() == null ? "" : employee.getFirstName(),
                employee.getLastName() == null ? "" : employee.getLastName()).trim();

        String statusMessage;
        switch (newStatus) {
            case "Resolved":
                statusMessage = "Your ticket has been <strong>resolved</strong>.";
                break;
            case "Rejected":
                statusMessage = "Your ticket has been <strong>rejected</strong>.";
                break;
            case "In Progress":
                statusMessage = "Your ticket is now <strong>in progress</strong>.";
                break;
            default:
                statusMessage = "Your ticket status has been updated to <strong>" + newStatus + "</strong>.";
        }

        String responseHtml = (ticket.getAdminResponse() != null && !ticket.getAdminResponse().trim().isEmpty())
                ? "<li><strong>Response:</strong> " + ticket.getAdminResponse() + "</li>"
                : "";

        String body = "<p>Hello " + employeeName + ",</p>"
                + "<p>" + statusMessage + "</p>"
                + "<ul>"
                + "<li><strong>Subject:</strong> " + ticket.getSubject() + "</li>"
                + "<li><strong>Status:</strong> " + newStatus + "</li>"
                + responseHtml
                + "</ul>"
                + "<p>If you have any questions, please contact HR.</p>";

        emailService.sendHtmlEmail(employee.getEmail(), mailFrom, null,
                "Your Support Ticket has been " + newStatus, body);
    }

    private String normalizeStatus(String status) {
        if (status == null) {
            throw new RuntimeException("Status is required");
        }
        String trimmed = status.trim();
        if ("open".equalsIgnoreCase(trimmed)) return "Open";
        if ("in progress".equalsIgnoreCase(trimmed)) return "In Progress";
        if ("resolved".equalsIgnoreCase(trimmed)) return "Resolved";
        if ("rejected".equalsIgnoreCase(trimmed)) return "Rejected";
        throw new RuntimeException("Invalid status: " + status);
    }

    private TicketDTO convertToDTO(Ticket ticket) {
        return new TicketDTO(
                ticket.getId(),
                ticket.getEmployee().getEmpId(),
                ticket.getEmployee().getFirstName() + " " + ticket.getEmployee().getLastName(),
                ticket.getSubject(),
                ticket.getDescription(),
                ticket.getStatus(),
                ticket.getAdminResponse(),
                ticket.getCreatedAt().format(DISPLAY_DATE_FORMATTER),
                ticket.getUpdatedAt().format(DISPLAY_DATE_FORMATTER)
        );
    }
}
