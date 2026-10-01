package com.employee.management.backend.config;

import com.employee.management.backend.Entity.Employee;
import com.employee.management.backend.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

// Creates a single default admin account on startup if the employees table has no admin yet,
// so a freshly-provisioned database always has a way to log in. Safe to run on every restart:
// it's a no-op once any admin account exists.
@Component
public class AdminAccountSeeder implements CommandLineRunner {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final DefaultClientProvider defaultClientProvider;
    private final String adminEmail;
    private final String adminPassword;

    public AdminAccountSeeder(EmployeeRepository employeeRepository,
                               PasswordEncoder passwordEncoder,
                               DefaultClientProvider defaultClientProvider,
                               @Value("${app.admin.seed-email:admin@hrms.local}") String adminEmail,
                               @Value("${app.admin.seed-password:Admin@123}") String adminPassword) {
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
        this.defaultClientProvider = defaultClientProvider;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        if (employeeRepository.existsByRoleIgnoreCase("admin")) {
            return;
        }

        Employee admin = new Employee();
        admin.setFirstName("System");
        admin.setLastName("Admin");
        admin.setEmail(adminEmail);
        admin.setRole("admin");
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setClient(defaultClientProvider.getOrCreate());
        employeeRepository.save(admin);

        System.out.println("=================================================================");
        System.out.println("No admin account existed - created a default one:");
        System.out.println("  Email:    " + adminEmail);
        System.out.println("  Password: " + adminPassword);
        System.out.println("Log in and change the password (or create a proper admin and delete");
        System.out.println("this one). Override app.admin.seed-email / app.admin.seed-password");
        System.out.println("to change what gets created here.");
        System.out.println("=================================================================");
    }
}
