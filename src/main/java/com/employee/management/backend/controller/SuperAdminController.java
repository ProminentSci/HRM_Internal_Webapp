package com.employee.management.backend.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.employee.management.backend.Entity.Client;
import com.employee.management.backend.Entity.Employee;
import com.employee.management.backend.Entity.SuperAdmin;
import com.employee.management.backend.repository.ClientRepository;
import com.employee.management.backend.repository.EmployeeRepository;
import com.employee.management.backend.repository.SuperAdminRepository;
import com.employee.management.backend.security.JwtUtil;
import com.employee.management.backend.service.EmployeeService;



/**
 * Platform-level administration: Super Admin manages Client companies (create, activate/
 * disable, view roster size) and never touches any client's HR data directly - that stays
 * behind the normal admin/employee endpoints, scoped per-client via SecurityUtils.currentClientId().
 */
@RestController
@RequestMapping("/api/super-admin")
public class SuperAdminController {

    private final SuperAdminRepository superAdminRepository;
    private final ClientRepository clientRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeService employeeService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public SuperAdminController(SuperAdminRepository superAdminRepository, ClientRepository clientRepository,
                                 EmployeeRepository employeeRepository, EmployeeService employeeService,
                                 PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.superAdminRepository = superAdminRepository;
        this.clientRepository = clientRepository;
        this.employeeRepository = employeeRepository;
        this.employeeService = employeeService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        if (request.email == null || request.email.isBlank() || request.password == null || request.password.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email and password are required"));
        }
        SuperAdmin admin = superAdminRepository.findByEmail(request.email.trim()).orElse(null);
        if (admin == null || !passwordEncoder.matches(request.password, admin.getPassword())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid email or password"));
        }
        // clientId is deliberately null here - a Super Admin isn't scoped to any single tenant.
        String token = jwtUtil.generateToken(admin.getId(), admin.getEmail(), "super_admin", null);
        return ResponseEntity.ok(Map.of(
                "userId", admin.getId(),
                "name", admin.getName() == null ? "" : admin.getName(),
                "email", admin.getEmail(),
                "role", "super_admin",
                "token", token
        ));
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/clients")
    public List<ClientDTO> getClients() {
        return clientRepository.findAllByOrderByCompanyNameAsc().stream().map(this::toDTO).toList();
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/clients/{id}")
    public ResponseEntity<?> getClient(@PathVariable Long id) {
        Client client = clientRepository.findById(id).orElse(null);
        if (client == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toDTO(client));
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PostMapping("/clients")
    public ResponseEntity<?> createClient(@RequestBody CreateClientRequest request) {
        if (request.companyName == null || request.companyName.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Company name is required"));
        }
        if (request.adminEmail == null || request.adminEmail.isBlank()
                || request.adminPassword == null || request.adminPassword.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "The company admin's email and password are required"));
        }
        if (employeeRepository.findByEmail(request.adminEmail.trim()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "An account with that email already exists"));
        }

        Client client = new Client();
        client.setCompanyName(request.companyName.trim());
        client.setContactEmail(request.contactEmail);
        client.setContactPhone(request.contactPhone);
        client.setStatus("Active");
        client.setCreatedAt(LocalDateTime.now());
        Client savedClient = clientRepository.save(client);

        Employee admin = new Employee();
        admin.setFirstName(request.adminFirstName);
        admin.setLastName(request.adminLastName);
        admin.setEmail(request.adminEmail.trim());
        admin.setRole("admin");
        admin.setPassword(passwordEncoder.encode(request.adminPassword));
        admin.setClient(savedClient);
        employeeService.createEmployee(admin);

        return ResponseEntity.ok(toDTO(savedClient));
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/clients/{id}/employees")
    public ResponseEntity<?> getClientEmployees(@PathVariable Long id,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "20") int size) {
        if (!clientRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        Page<Employee> employees = employeeRepository.findByClientId(id,
                PageRequest.of(Math.max(page, 0), Math.max(size, 1)));
        List<EmployeeSummaryDTO> content = employees.getContent().stream().map(this::toEmployeeSummary).toList();
        return ResponseEntity.ok(Map.of(
                "content", content,
                "totalElements", employees.getTotalElements(),
                "totalPages", employees.getTotalPages()
        ));
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PatchMapping("/clients/{id}/status")
    public ResponseEntity<?> updateClientStatus(@PathVariable Long id, @RequestBody StatusRequest request) {
        Client client = clientRepository.findById(id).orElse(null);
        if (client == null) {
            return ResponseEntity.notFound().build();
        }
        if (request.status == null || request.status.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "status is required"));
        }
        client.setStatus(request.status.trim());
        Client saved = clientRepository.save(client);
        return ResponseEntity.ok(toDTO(saved));
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/dashboard")
    public Map<String, Object> getDashboard() {
        List<Client> clients = clientRepository.findAll();
        long activeClients = clients.stream().filter(c -> "Active".equalsIgnoreCase(c.getStatus())).count();
        return Map.of(
                "totalClients", clients.size(),
                "activeClients", activeClients,
                "disabledClients", clients.size() - activeClients,
                "totalEmployees", employeeRepository.count()
        );
    }

    private EmployeeSummaryDTO toEmployeeSummary(Employee employee) {
        EmployeeSummaryDTO dto = new EmployeeSummaryDTO();
        dto.empId = employee.getEmpId();
        dto.name = String.format("%s %s",
                employee.getFirstName() == null ? "" : employee.getFirstName(),
                employee.getLastName() == null ? "" : employee.getLastName()).trim();
        dto.email = employee.getEmail();
        dto.role = employee.getRole();
        return dto;
    }

    private ClientDTO toDTO(Client client) {
        ClientDTO dto = new ClientDTO();
        dto.id = client.getId();
        dto.companyName = client.getCompanyName();
        dto.contactEmail = client.getContactEmail();
        dto.contactPhone = client.getContactPhone();
        dto.status = client.getStatus();
        dto.createdAt = client.getCreatedAt() != null ? client.getCreatedAt().toString() : null;
        dto.employeeCount = employeeRepository.countByClientId(client.getId());
        return dto;
    }

    public static class LoginRequest {
        public String email;
        public String password;
    }

    public static class CreateClientRequest {
        public String companyName;
        public String contactEmail;
        public String contactPhone;
        public String adminFirstName;
        public String adminLastName;
        public String adminEmail;
        public String adminPassword;
    }

    public static class StatusRequest {
        public String status;
    }

    public static class EmployeeSummaryDTO {
        public Long empId;
        public String name;
        public String email;
        public String role;
    }

    public static class ClientDTO {
        public Long id;
        public String companyName;
        public String contactEmail;
        public String contactPhone;
        public String status;
        public String createdAt;
        public long employeeCount;
    }
}
